package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.MaxResponseTime;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.HttpRequestMetrics;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import it.polimi.saefa.knowledge.rest.AddAdaptationParameterValueRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Slf4j
@org.springframework.stereotype.Service
public class AnalyseService { //todo forse le instanceStats non servono più
    //private Date lastAnalysisTimestamp = Date.from(Instant.ofEpochMilli(0));
    //private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final List<AdaptationOption> adaptationOptions = new ArrayList<>();
    private final List<List<AdaptationOption>> adaptationOptionsWindow = new LinkedList<>();

    @Value("${ANALYSIS_WINDOW_SIZE}")
    private int analysisWindowSize;
    //Number of new metrics to analyse for each instance of each service
    @Value("${METRICS_WINDOW_SIZE}")
    private int metricsWindowSize;
    @Value("${FAILURE_RATE_THRESHOLD}")
    private double failureRateThreshold;
    @Value("${UNREACHABLE_RATE_THRESHOLD}")
    private double unreachableRateThreshold;

    // Variables to temporary store the new values specified by an admin until they are applied during the next loop iteration
    private Integer newMetricsWindowSize;
    private Integer newAnalysisWindowSize;
    private Double newFailureRateThreshold;
    private Double newUnreachableRateThreshold;


    @Autowired
    private KnowledgeClient knowledgeClient;

    /*
    public AnalyseService(@Value("${ADAPTATION_WINDOW_SIZE}") int adaptationOptionsWindowSize) {
        this.adaptationOptionsWindowSize = adaptationOptionsWindowSize;
        adaptationOptionsWindow = new List[adaptationOptionsWindowSize];
    }
    */

    public void startAnalysis() {
        log.warn("Starting analysis");
        updateWindowAndThresholds(); //update window size and thresholds if they have been changed from an admin
        List<Service> currentArchitecture = knowledgeClient.getServices();
        //List<ServiceStats> currentArchitectureStats = new LinkedList<>();
        updateWindowAndThresholds();
        for (Service service : currentArchitecture) {
            //ServiceStats serviceStats = new ServiceStats(service);
            List<InstanceStats> instancesStats = new ArrayList<>();
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN)
                    continue;
                List<InstanceMetrics> metrics = new LinkedList<>();
                /*
                int afterMetrics = analysisWindowStep;
                metrics.addAll(knowledgeClient.getLatestNMetricsBeforeDate(instance.getInstance(), dateFormatter.format(lastAnalysisTimestamp), metricsWindowSize - analysisWindowStep));
                if (metrics.size() != metricsWindowSize - analysisWindowStep) {
                    afterMetrics = metricsWindowSize - metrics.size();
                }
                metrics.addAll(knowledgeClient.getLatestNMetricsAfterDate(instance.getInstance(), dateFormatter.format(lastAnalysisTimestamp), afterMetrics));
                 */
                metrics.addAll(knowledgeClient.getLatestNMetricsOfCurrentInstance(instance.getInstanceId(), metricsWindowSize));
                // Not enough data to perform analysis
                if (metrics.size() != metricsWindowSize) {
                    instancesStats.add(new InstanceStats(instance)); // Add empty instance stats that will be filled with the average valuesStackHistory computed over the other instances

                    continue;
                }
                //per ciascuna metrica, devo prendere tutti gli endpoint e contare le richieste fallite e le richieste andate a buon fine

                /*se c'è una metrica unreachable abbiamo 4 casi, riassumibili con la seguente regex: UNREACHABLE+ FAILED* ACTIVE*
                Se c'è una failed di mezzo, la consideriamo come il caso di sotto.
                Se finisce con active, tutto a posto a meno di anomalie nell'unreachable rate


                //se c'è una failed ma non una unreachable, abbiamo 2 casi, riassumibili con la seguente regex: FAILED+ ACTIVE*.
                Caso solo failed: considerare l'istanza come spenta, ignorarla nel calcolo degli adaptation parameters.
                Caso last metric active: tutto ok, a meno di conti sul tasso di faild e unreachable

                Calcolare unreachable rate e failure rate.
                Se superiori a delle soglie, considerarle faulty e quindi da spegnere e non cosniderare nel calcolo dell'avilability


                 */
                InstanceMetrics latestMetrics = metrics.get(0);
                //InstanceMetrics oldestMetrics = metrics.get(metrics.size() - 1);

                failureRateThreshold = Math.min(failureRateThreshold, 1.0);
                unreachableRateThreshold = Math.min(unreachableRateThreshold, 1.0);

                double failureRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isFailed() ? 1:0), Double::sum) / metrics.size();
                double unreachableRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isUnreachable() ? 1:0), Double::sum) / metrics.size();
                double inactiveRate = failureRate + unreachableRate;

                if (latestMetrics.isFailed() || unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold || inactiveRate>=1) { //in ordine di probabilità
                    //TODO Prova a spegnere l'istanza forzatamente, non aspettarti una risposta.
                    // !!!C'è bisogno che l'istanza va marcata come shutdown!!!.
                    // Discorso dipendenze: provare ad approfondire
                    continue;

                    /*
                    Se l'utlima metrica è failed, allora l'istanza è crashata. Va marcata come istanza spenta per non
                    confonderla con una potenziale nuova istanza con stesso identificatore.
                    Inoltre, per evitare comportamenti oscillatori, scegliamo di terminare istanze poco reliable che
                    sono spesso unreachable o failed.
                     */
                }

                List<InstanceMetrics> activeMetrics = metrics.stream().filter(InstanceMetrics::isActive).toList(); //la lista contiene almeno un elemento grazie all'inactive rate
                InstanceMetrics oldestActiveMetrics = activeMetrics.get(activeMetrics.size() - 1);
                InstanceMetrics latestActiveMetrics = activeMetrics.get(0);

                if(oldestActiveMetrics == latestActiveMetrics){ //non ci sono abbastanza metriche per questa istanza
                    instancesStats.add(new InstanceStats(instance)); // Add empty instance stats that will be filled with the average valuesStackHistory computed over the other instances

                    continue;
                }

                // <endpoint, value>
                Map<String, Double> endpointAvgRespTime = new HashMap<>();
                Map<String, Double> endpointMaxRespTime = new HashMap<>();

                for (String endpoint : oldestActiveMetrics.getHttpMetrics().keySet()) {
                    double durationDifference = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDuration() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDuration();
                    double requestDifference = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount();
                    if (requestDifference != 0)
                        endpointAvgRespTime.put(endpoint, durationDifference / requestDifference);
                    endpointMaxRespTime.put(endpoint, latestActiveMetrics.getHttpMetrics().get(endpoint).getMaxDuration());
                }

                // Single instance statistics (i.e., valuesStackHistory that the analysis use to compute adaptation options)
                InstanceStats instanceStats = new InstanceStats(instance,
                        computeInstanceAvgResponseTime(endpointAvgRespTime),
                        computeInstanceMaxResponseTime(endpointMaxRespTime),
                        computeInstanceAvailability(oldestActiveMetrics, latestActiveMetrics));
                instancesStats.add(instanceStats);
            }
            updateServiceStats(service, instancesStats); //TODO e se sono tutte empty? Che famo? Il loop prosegue senza adattamento
            //currentArchitectureStats.add(serviceStats); //TODO
        }
        //serviceStatsHistory.add(currentArchitectureStats);

        /*if(!serviceStatsHistory.isFull())
            return;

         */



        log.warn("Ending analysis");
        //serviceStatsHistory.clear();
        //creare set di possibili istruzioni da mandare al Plan
    }

    private void handleAvailabilityAnalysis() {

        /*
        for(List<ServiceStats> singleAnalysisIteration : serviceStatsHistory){ //map invece di window, con service id e lista di options

            for(ServiceStats serviceStats : singleAnalysisIteration){
            }
        }

        */

    }

    private void handleMaxResponseTime() {
        //TODO
    }

    private Double computeInstanceAvailability(InstanceMetrics firstMetrics, InstanceMetrics lastMetrics) {
        double successfulRequests = 0;
        double failedRequests = 0;

        for(HttpRequestMetrics httpMetric : lastMetrics.getHttpMetrics().values()){
            for(HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()){
                if(outcomeMetric.getOutcome().equalsIgnoreCase("success")){ //todo controllare gli endpoint che ritornano 100 o 300. O MEGLIO, fare solo server error e non anche client error (400)
                    successfulRequests += outcomeMetric.getCount();
                } else {
                    failedRequests += outcomeMetric.getCount();
                }
            }
        }

        for(HttpRequestMetrics httpMetric : firstMetrics.getHttpMetrics().values()){
            for(HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()){
                if(outcomeMetric.getOutcome().equalsIgnoreCase("success")){ //todo vedi sopra
                    successfulRequests -= outcomeMetric.getCount();
                } else {
                    failedRequests -= outcomeMetric.getCount();
                }
            }
        }

        return successfulRequests == 0 ? null : successfulRequests / (successfulRequests + failedRequests);
    }

    private Double computeInstanceMaxResponseTime(Map<String, Double> endpointMaxRespTime) {
        return endpointMaxRespTime.values().stream().max(Double::compareTo).orElse(null);
    }

    private Double computeInstanceAvgResponseTime(Map<String, Double> endpointAvgRespTime) {
        double toReturn =  endpointAvgRespTime.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return toReturn == 0.0 ? null : toReturn;
    }


    public void changeMetricsWindow(int window) {
        this.newMetricsWindowSize = window;
    }

    public void changeAnalysisWindow(int window) {
        this.newAnalysisWindowSize = window;
    }

    public void changeFailureRateThreshold(double failureRateThreshold) {
        this.newFailureRateThreshold = failureRateThreshold;
    }

    public void changeUnreachableRateThreshold(double unreachableRateThreshold) {
        this.newUnreachableRateThreshold = unreachableRateThreshold;
    }



    private void updateServiceStats(Service service, List<InstanceStats> instancesStats){
        if (instancesStats.isEmpty())
            return;
        Double averageAvailability, averageMaxResponseTime, averageResponseTime;
        List<InstanceStats> emptyStats = new LinkedList<>();
        double availabilityAccumulator = 0;
        double maxResponseTimeAccumulator = 0;
        double averageResponseTimeAccumulator = 0;
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isDataUnavailable()) {
                emptyStats.add(instanceStats);
            } else {
                availabilityAccumulator += instanceStats.getAvailability();
                maxResponseTimeAccumulator += instanceStats.getMaxResponseTime();
                averageResponseTimeAccumulator += instanceStats.getAverageResponseTime();
            }
        }

        if(emptyStats.size() == instancesStats.size()) {
            return;
        }

        averageAvailability = availabilityAccumulator / (instancesStats.size() - emptyStats.size());
        averageMaxResponseTime = maxResponseTimeAccumulator / (instancesStats.size() - emptyStats.size());
        averageResponseTime = averageResponseTimeAccumulator / (instancesStats.size() - emptyStats.size());

        for (InstanceStats instanceStats : emptyStats) {
            instanceStats.setAvailability(averageAvailability);
            instanceStats.setMaxResponseTime(averageMaxResponseTime);
            instanceStats.setAverageResponseTime(averageResponseTime);
        }

        for(InstanceStats instanceStats : instancesStats){
            instanceStats.getInstance().getAdaptationParamCollection().addNewAdaptationParamValue(Availability.class, instanceStats.getAvailability());
            instanceStats.getInstance().getAdaptationParamCollection().addNewAdaptationParamValue(MaxResponseTime.class, instanceStats.getMaxResponseTime());
            instanceStats.getInstance().getAdaptationParamCollection().addNewAdaptationParamValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
            knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(instanceStats.getInstance().getServiceId(), instanceStats.getInstance().getInstanceId(), Availability.class, instanceStats.getAvailability()));
            knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(instanceStats.getInstance().getServiceId(), instanceStats.getInstance().getInstanceId(), MaxResponseTime.class, instanceStats.getMaxResponseTime()));
            knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(instanceStats.getInstance().getServiceId(), instanceStats.getInstance().getInstanceId(), AverageResponseTime.class, instanceStats.getAverageResponseTime()));
        }

        AdaptationParamCollection currentImplementationParamCollection = service.getCurrentImplementationObject().getAdaptationParamCollection();

        currentImplementationParamCollection.addNewAdaptationParamValue(AverageResponseTime.class, averageResponseTime);
        currentImplementationParamCollection.addNewAdaptationParamValue(MaxResponseTime.class,instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow());
        currentImplementationParamCollection.addNewAdaptationParamValue(Availability.class, (1 - instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1 - val))));

        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), AverageResponseTime.class, averageResponseTime));
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), MaxResponseTime.class,instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow()));
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), Availability.class, (1 - instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1 - val)))));



        log.warn("Param collection : " + currentImplementationParamCollection.getAdaptationParamHistories());


        //availability.setAverageAvailability(averageAvailability); TODO va aggiunta nelle adaptation options
    }

    private void updateWindowAndThresholds() {
        if (newMetricsWindowSize != null) {
            this.metricsWindowSize = newMetricsWindowSize;
            newMetricsWindowSize = null;
        }
        if (newFailureRateThreshold != null) {
            failureRateThreshold = newFailureRateThreshold;
            newFailureRateThreshold = null;
        }
        if (newUnreachableRateThreshold != null) {
            unreachableRateThreshold = newUnreachableRateThreshold;
            newUnreachableRateThreshold = null;
        }
        if (newAnalysisWindowSize != null) {
            analysisWindowSize = newAnalysisWindowSize;
            newAnalysisWindowSize = null;
        }
    }




    /*

    private void updateAverageResponseTime(Service service, List<InstanceStats> instancesStats) {
        AverageResponseTime averageResponseTime = service.getAdaptationParameter(AverageResponseTime.class);
        if (!instancesStats.isEmpty())
            averageResponseTime.setValue(instancesStats.stream().mapToDouble(InstanceStats::getAverageResponseTime).average().orElseThrow());
    }

    private void updateMaxResponseTime(Service service, List<InstanceStats> instancesStats) {
        MaxResponseTime maxResponseTime = service.getAdaptationParameter(MaxResponseTime.class);
        if (!instancesStats.isEmpty())
            maxResponseTime.setValue(instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow());
    }

    private void updateAvailability(Service service, List<InstanceStats> instancesStats) {
        Availability availability = service.getAdaptationParameter(Availability.class);
        if (!instancesStats.isEmpty()) {
            availability.setValue(1 - instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1 - val)));

        }
    }

     */
}
