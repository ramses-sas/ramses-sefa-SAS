package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.AdaptationOption;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.HttpRequestMetrics;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@org.springframework.stereotype.Service
public class AnalyseService {
    //private Date lastAnalysisTimestamp = Date.from(Instant.ofEpochMilli(0));
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final ServiceStatsWindow serviceStatsHistory;
    private final List<AdaptationOption> adaptationOptions = new ArrayList<>();

    //Sliding window of size `analysisWindowSize`, containing `analysisWindowStep` new metrics
    private int analysisWindowSize;
    @Value("${FAILURE_RATE_THRESHOLD}")
    private double failureRateThreshold;
    @Value("${UNREACHABLE_RATE_THRESHOLD}")
    private double unreachableRateThreshold;

    private Integer newWindowSize;
    private Double newFailureRateThreshold;
    private Double newUnreachableRateThreshold;


    @Autowired
    private KnowledgeClient knowledgeClient;

    public AnalyseService(@Value("${ANALYSIS_WINDOW_SIZE}") int analysisWindowSize) {
        this.analysisWindowSize = analysisWindowSize;
        serviceStatsHistory = new ServiceStatsWindow(analysisWindowSize);

    }

    public void startAnalysis() {
        log.warn("Starting analysis");
        List<Service> currentArchitecture = knowledgeClient.getServices();
        List<ServiceStats> currentArchitectureStats = new LinkedList<>();
        updateWindowAndThresholds();
        for (Service service : currentArchitecture) {
            ServiceStats serviceStats = new ServiceStats(service);
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                if(instance.getCurrentStatus() == InstanceStatus.SHUTDOWN) {
                    continue;
                }
                List<InstanceMetrics> metrics = new LinkedList<>();
                /*
                int afterMetrics = analysisWindowStep;
                metrics.addAll(knowledgeClient.getLatestNMetricsBeforeDate(instance.getInstance(), dateFormatter.format(lastAnalysisTimestamp), analysisWindowSize - analysisWindowStep));
                if (metrics.size() != analysisWindowSize - analysisWindowStep) {
                    afterMetrics = analysisWindowSize - metrics.size();
                }
                metrics.addAll(knowledgeClient.getLatestNMetricsAfterDate(instance.getInstance(), dateFormatter.format(lastAnalysisTimestamp), afterMetrics));
                 */
                metrics.addAll(knowledgeClient.getLatestNMetricsOfCurrentInstance(instance.getInstanceId(), analysisWindowSize));
                // Not enough data to perform analysis
                if (metrics.size() != analysisWindowSize) {
                    serviceStats.addInstanceStats(new InstanceStats(instance)); // Add empty instance stats that will be filled with the average values computed over the other instances
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

                if(latestMetrics.isFailed() || unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold || inactiveRate>=1) { //in ordine di probabilità
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
                    serviceStats.addInstanceStats(new InstanceStats(instance)); // Add empty instance stats that will be filled with the average values computed over the other instances
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

                // Single instance statistics (i.e., values that the analysis use to compute adaptation options)
                InstanceStats instanceStats = new InstanceStats(instance,
                        computeInstanceAvgResponseTime(endpointAvgRespTime),
                        computeInstanceMaxResponseTime(endpointMaxRespTime),
                        computeInstanceAvailability(oldestActiveMetrics, latestActiveMetrics));
                serviceStats.addInstanceStats(instanceStats);
            }
            serviceStats.updateStats(); //TODO e se sono tutte empty? Che famo? Il loop prosegue senza adattamento
            currentArchitectureStats.add(serviceStats);
        }
        serviceStatsHistory.add(currentArchitectureStats);

    //quando fai adattamento, devi togliere dalla window le cose relative a quel servizio


        log.warn("Ending analysis");
        //lastAnalysisTimestamp = new Date();
        //creare set di possibili istruzioni da mandare al Plan
    }

    private void handleAvailabilityAnalysis() {

    }

    private void handleMaxResponseTime(ServiceStats serviceStats) {
        //TODO
    }

    private Double computeInstanceAvailability(InstanceMetrics firstMetrics, InstanceMetrics lastMetrics) {
        double successfulRequests = 0;
        double failedRequests = 0;

        for(HttpRequestMetrics httpMetric : lastMetrics.getHttpMetrics().values()){
            for(HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()){
                if(outcomeMetric.getOutcome().equalsIgnoreCase("success")){ //todo controllare gli endpoint che ritornano 100 o 300
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

        return successfulRequests / (successfulRequests + failedRequests);
    }

    private Double computeInstanceMaxResponseTime(Map<String, Double> endpointMaxRespTime) {
        return endpointMaxRespTime.values().stream().max(Double::compareTo).orElse(null);
    }

    private Double computeInstanceAvgResponseTime(Map<String, Double> endpointAvgRespTime) {
        return endpointAvgRespTime.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }


    public void changeWindow(int window) {
        this.newWindowSize = window;
    }

    public void changeFailureRateThreshold(double failureRateThreshold) {
        this.newFailureRateThreshold = failureRateThreshold;
    }

    public void changeUnreachableRateThreshold(double unreachableRateThreshold) {
        this.newUnreachableRateThreshold = unreachableRateThreshold;
    }

    private void updateWindowAndThresholds() {
        if(newWindowSize != null) {
            this.analysisWindowSize = newWindowSize;
            newWindowSize = null;
        }
        if (newFailureRateThreshold != null) {
            failureRateThreshold = newFailureRateThreshold;
            newFailureRateThreshold = null;
        }
        if (newUnreachableRateThreshold != null) {
            unreachableRateThreshold = newUnreachableRateThreshold;
            newUnreachableRateThreshold = null;
        }
    }

}
