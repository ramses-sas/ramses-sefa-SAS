package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AddInstances;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.RemoveInstance;
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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Getter
@Setter
@Slf4j
@org.springframework.stereotype.Service
public class AnalyseService {
    //Number of analysis iterations to do before choosing the best adaptation options
    @Value("${ANALYSIS_WINDOW_SIZE}")
    private int analysisWindowSize;
    //Number of new metrics to analyse for each instance of each service
    @Value("${METRICS_WINDOW_SIZE}")
    private int metricsWindowSize;
    @Value("${FAILURE_RATE_THRESHOLD}")
    private double failureRateThreshold;
    @Value("${UNREACHABLE_RATE_THRESHOLD}")
    private double unreachableRateThreshold;
    @Value("${PARAMETERS_SATISFACTION_RATE}")
    private double parametersSatisfactionRate;

    private int analysisIterationCounter = 0;

    // Variables to temporary store the new values specified by an admin until they are applied during the next loop iteration
    private Integer newMetricsWindowSize;
    private Integer newAnalysisWindowSize;
    private Double newFailureRateThreshold;
    private Double newUnreachableRateThreshold;

    // <serviceId, Service>
    private Map<String, Service> currentArchitectureMap;
    // <serviceId, ServiceStats>
    private Map<String, ServiceStats> servicesStatsMap;


    @Autowired
    private KnowledgeClient knowledgeClient;


    public void startAnalysis() {
        log.warn("Starting analysis");
        updateWindowAndThresholds(); //update window size and thresholds if they have been changed from an admin
        currentArchitectureMap = knowledgeClient.getServices().stream().collect(HashMap::new, (m, v) -> m.put(v.getServiceId(), v), HashMap::putAll);
        servicesStatsMap = new HashMap<>();
        final Collection<Service> currentArchitecture = currentArchitectureMap.values();
        for (Service service : currentArchitecture) {
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
                // Not enough data to perform analysis. Can happen only at startup because once you have enough data, you will always have enough data
                if (metrics.size() != metricsWindowSize) {
                    instancesStats.add(new InstanceStats(instance)); // Add unavailable instance stats that will be filled with the average valuesStackHistory computed over the other instances
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

                failureRateThreshold = Math.min(failureRateThreshold, 1.0);
                unreachableRateThreshold = Math.min(unreachableRateThreshold, 1.0);

                double failureRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isFailed() ? 1:0), Double::sum) / metrics.size();
                double unreachableRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isUnreachable() ? 1:0), Double::sum) / metrics.size();
                double inactiveRate = failureRate + unreachableRate;

                if (latestMetrics.isFailed() || unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold || inactiveRate >= 1) { //in ordine di probabilità
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
                if (activeMetrics.size() < 2) { //non ci sono abbastanza metriche per questa istanza, scelta ottimistica di considerarla come istanza attiva (con parametri medi). Al massimo prima o poi verrà punita.
                    instancesStats.add(new InstanceStats(instance)); // Add unavailable instance stats that will be filled with the average valuesStackHistory computed over the other instances
                    continue;
                }
                InstanceMetrics oldestActiveMetrics = activeMetrics.get(activeMetrics.size() - 1);
                InstanceMetrics latestActiveMetrics = activeMetrics.get(0);

                // <endpoint, value>
                Map<String, Double> endpointAvgRespTime = new HashMap<>();
                Map<String, Double> endpointMaxRespTime = new HashMap<>();

                for (String endpoint : oldestActiveMetrics.getHttpMetrics().keySet()) {
                    double durationDifference = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDurationOfSuccessful() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDurationOfSuccessful();
                    double requestDifference = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
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
            if (instancesStats.isEmpty()) {
                // TODO alloca nuova istanza creando una adaptation option (l'unica per questo servizio)
                continue;
            }
            // Given the adaptation parameters of each service instance, compute the adaptation parameters for the service
            ServiceStats serviceStats = createServiceStats(instancesStats);
            // The serviceStats are not available if all the instances of the service have unavailable stats.
            // In this case none of the instances have enough metrics to perform the analysis.
            if (serviceStats == null)
                continue;
            // Update the stats of the service and of its instances both locally and remotely
            updateServiceAndInstancesWithStats(service, serviceStats, instancesStats);
            servicesStatsMap.put(service.getServiceId(), serviceStats);
        }


        if (analysisIterationCounter == analysisWindowSize) {
            analysisIterationCounter = 0;
            // <serviceId>
            Set<String> analysedServices = new HashSet<>();
            List<AdaptationOption> adaptationOptions = new LinkedList<>();
            for (Service service : currentArchitecture) {
                adaptationOptions.addAll(computeAdaptationOptions(service, analysedServices));
            }
            log.debug("Adaptation options: {}", adaptationOptions);
            // SEND THE ADAPTATION OPTIONS TO THE KNOWLEDGE FOR THE PLAN
            knowledgeClient.proposeAdaptationOptions(adaptationOptions);
        } else {
            analysisIterationCounter++;
        }
        log.warn("Ending analysis");
    }

    private List<AdaptationOption> computeAdaptationOptions(Service service, Set<String> analysedServices) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        if (service == null)
            return adaptationOptions;
        ServiceStats serviceStats = servicesStatsMap.get(service.getServiceId());
        if (serviceStats == null || analysedServices.contains(service.getServiceId()))
            return adaptationOptions;
        analysedServices.add(service.getServiceId());
        List<Service> serviceDependencies = service.getDependencies().stream().map(currentArchitectureMap::get).toList();
        for (Service s : serviceDependencies) {
            adaptationOptions.addAll(computeAdaptationOptions(s, analysedServices));
        }

        // Se le dipendenze del servizio corrente hanno problemi non analizzo me stesso ma provo prima a risolvere i problemi delle dipendenze
        // ergo la lista di adaptation option non contiene adaptation option riguardanti il servizio corrente
        if (!adaptationOptions.isEmpty())
            return adaptationOptions;

        // Analisi del servizio corrente, se non ha dipendenze con problemi
        // TODO
        // HERE THE LOGIC FOR CHOOSING THE ADAPTATION OPTIONS
        adaptationOptions.addAll(handleAvailabilityAnalysis(service, serviceStats.getAverageAvailability()));

        return adaptationOptions;
    }

    private List<AdaptationOption> handleAvailabilityAnalysis(Service service, double averageAvailability) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        List<Double> serviceAvailabilities = service.getCurrentImplementationObject().getAdaptationParamCollection().getLatestNAdaptationParamValues(Availability.class, analysisWindowSize);
        if (serviceAvailabilities != null && !service.getAdaptationParamSpecifications().get(Availability.class).isSatisfied(serviceAvailabilities, parametersSatisfactionRate)) {
            // Order the instances by average availability (ascending)
            List<Instance> instances = service.getInstances().stream()
                    .sorted(Comparator.comparingDouble(i -> i.getAdaptationParamCollection().getLatestNAdaptationParamValues(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow())).toList();
            Instance worstInstance = instances.get(0);
            // 2 adaptation options: add N instances and remove the worst instance. Their benefits will be evaluated by the Plan
            adaptationOptions.add(new AddInstances(service.getServiceId(), service.getCurrentImplementation(), averageAvailability));
            // Ha il senso di "proponi di rimuovere l'istanza con l'availability peggiore. Se il constraint sull'avail continua a essere soddisfatto, hai risparmiato un'istanza"
            // TODO non va qui, perché questa proposta l'analisi deve valutarla se il constraint è soddisfatto, non se non lo è
            adaptationOptions.add(new RemoveInstance(service.getServiceId(), service.getCurrentImplementation(), worstInstance.getInstanceId()));
            // TODO mancano le considerazioni sul cambio di implementazione
        }
        return adaptationOptions;
    }


    private Double computeInstanceAvailability(InstanceMetrics firstMetrics, InstanceMetrics lastMetrics) {
        double successfulRequests = 0;
        double failedRequests = 0;

        for (HttpRequestMetrics httpMetric : lastMetrics.getHttpMetrics().values()) {
            for (HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()) {
                // We consider "successful" requests every request with a response code not in the 5xx range
                if (outcomeMetric.getStatus() < 500) {
                    successfulRequests += outcomeMetric.getCount();
                } else {
                    failedRequests += outcomeMetric.getCount();
                }
            }
        }

        for (HttpRequestMetrics httpMetric : firstMetrics.getHttpMetrics().values()) {
            for (HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()) {
                // We consider "successful" requests every request with a response code not in the 5xx range
                if (outcomeMetric.getStatus() < 500) {
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

    // Fill the unavailable stats with the average of the other instances stats and create the stats for the service.
    // Return null if all the instances have unavailable stats
    private ServiceStats createServiceStats(List<InstanceStats> instancesStats) {
        Double averageAvailability, averageMaxResponseTime, averageResponseTime;
        List<InstanceStats> unavailableStats = new LinkedList<>();
        double availabilityAccumulator = 0;
        double maxResponseTimeAccumulator = 0;
        double averageResponseTimeAccumulator = 0;
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isDataUnavailable()) {
                unavailableStats.add(instanceStats);
            } else {
                availabilityAccumulator += instanceStats.getAvailability();
                maxResponseTimeAccumulator += instanceStats.getMaxResponseTime();
                averageResponseTimeAccumulator += instanceStats.getAverageResponseTime();
            }
        }

        // All the instances have unavailable stats. So no adaptation is performed
        if (unavailableStats.size() == instancesStats.size())
            return null;

        averageAvailability = availabilityAccumulator / (instancesStats.size() - unavailableStats.size());
        averageMaxResponseTime = maxResponseTimeAccumulator / (instancesStats.size() - unavailableStats.size());
        averageResponseTime = averageResponseTimeAccumulator / (instancesStats.size() - unavailableStats.size());

        for (InstanceStats instanceStats : unavailableStats) {
            instanceStats.setAvailability(averageAvailability);
            instanceStats.setMaxResponseTime(averageMaxResponseTime);
            instanceStats.setAverageResponseTime(averageResponseTime);
        }
        Double availability = 1 - instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1 - val));
        Double maxResponseTime = instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow();
        return new ServiceStats(averageAvailability, averageResponseTime, averageMaxResponseTime, availability, maxResponseTime);
    }

    private void updateServiceAndInstancesWithStats(Service service, ServiceStats serviceStats, List<InstanceStats> instancesStats) {
        // Update the stats of the instances of the service both locally and remotely
        for (InstanceStats instanceStats : instancesStats) {
            instanceStats.getInstance().getAdaptationParamCollection().addNewAdaptationParamValue(Availability.class, instanceStats.getAvailability());
            instanceStats.getInstance().getAdaptationParamCollection().addNewAdaptationParamValue(MaxResponseTime.class, instanceStats.getMaxResponseTime());
            instanceStats.getInstance().getAdaptationParamCollection().addNewAdaptationParamValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
            knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(service.getServiceId(), instanceStats.getInstance().getInstanceId(), Availability.class, instanceStats.getAvailability()));
            knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(service.getServiceId(), instanceStats.getInstance().getInstanceId(), MaxResponseTime.class, instanceStats.getMaxResponseTime()));
            knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(service.getServiceId(), instanceStats.getInstance().getInstanceId(), AverageResponseTime.class, instanceStats.getAverageResponseTime()));
        }

        // Update the stats of the service both locally and remotely
        AdaptationParamCollection currentImplementationParamCollection = service.getCurrentImplementationObject().getAdaptationParamCollection();
        currentImplementationParamCollection.addNewAdaptationParamValue(AverageResponseTime.class, serviceStats.getAverageResponseTime());
        currentImplementationParamCollection.addNewAdaptationParamValue(MaxResponseTime.class, serviceStats.getMaxResponseTime());
        currentImplementationParamCollection.addNewAdaptationParamValue(Availability.class, serviceStats.getAvailability());
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), AverageResponseTime.class, serviceStats.getAverageResponseTime()));
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), MaxResponseTime.class, serviceStats.getMaxResponseTime()));
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), Availability.class, serviceStats.getAvailability()));
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
