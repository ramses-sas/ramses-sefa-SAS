package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.analyse.externalInterfaces.PlanClient;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.options.AddInstances;
import it.polimi.saefa.knowledge.domain.adaptation.options.ChangeLoadBalancerWeights;
import it.polimi.saefa.knowledge.domain.adaptation.options.RemoveInstance;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.MaxResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.metrics.HttpRequestMetrics;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
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

    @Autowired
    private PlanClient planClient;


    public void startAnalysis() {
        log.debug("Starting analysis");
        knowledgeClient.notifyModuleStart(Modules.ANALYSE);
        updateWindowAndThresholds(); //update window size and thresholds if they have been changed from an admin
        currentArchitectureMap = knowledgeClient.getServicesMap();
        List<AdaptationOption> forcedAdaptationOptions, proposedAdaptationOptions;
        forcedAdaptationOptions = analyse();
        proposedAdaptationOptions = adapt();
        proposedAdaptationOptions.addAll(forcedAdaptationOptions);
        knowledgeClient.proposeAdaptationOptions(proposedAdaptationOptions);
        updateAdaptationParamCollectionsInKnowledge();
        log.debug("Ending analysis and adaptation. Notifying the Plan to start the next iteration.");
        planClient.start();
    }

    // Given the available metrics, creates a new AdaptationParameterValue for all the instances when possible, and uses
    // their value to compute each new AdaptationParameterValue of the services. It also computes a list of
    // forced Adaptation Options to be applied immediately, as the creation (or removal) of instances upon failures.
    //TODO ho scordato qualcosa?
    public List<AdaptationOption> analyse(){
        servicesStatsMap = new HashMap<>();
        Collection<Service> currentArchitecture = currentArchitectureMap.values();
        List<AdaptationOption> adaptationOptions = new ArrayList<>();
        for (Service service : currentArchitecture) {
            List<InstanceStats> instancesStats = new ArrayList<>();
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                // Ignore shutdown instances (they will disappear from the architecture map in the next iterations)
                if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN)
                    continue;

                List<InstanceMetrics> metrics = new LinkedList<>(
                        knowledgeClient.getLatestNMetricsOfCurrentInstance(instance.getInstanceId(), metricsWindowSize));

                // Not enough data to perform analysis. Can happen only at startup and after an adaptation.
                // If there is at least one value in the stack of adaptation parameters, we do not compute a new one and the managing will use the last one (done in the instanceStats constructor)
                // Otherwise, the instance is a new one, and we assign to it the adaptation parameters of the oracle or of the service (done in the createServiceStatsAndFillJustBornInstances method)
                if (metrics.size() != metricsWindowSize) {
                    // TODO se passiamo all'oracolo le empty stats vanno riempite con i dati dell'oracolo

                    // If it's a new instance, it will have the adaptation parameters of the service or of the oracle.
                    // If it's not a new instance, it will have the latest available adaptation parameters
                    instancesStats.add(new InstanceStats(instance));
                    continue;
                }

                /*se c'è una metrica unreachable abbiamo 4 casi, riassumibili con la seguente regex: UNREACHABLE+ FAILED* ACTIVE*
                Se c'è una failed di mezzo, la consideriamo come il caso di sotto.
                Se finisce con active, tutto a posto a meno di anomalie nell'unreachable rate

                //se c'è una failed ma non una unreachable, abbiamo 2 casi, riassumibili con la seguente regex: FAILED+ ACTIVE*.
                Caso solo failed: considerare l'istanza come spenta, ignorarla nel calcolo degli adaptation parameters.
                Caso last metric active: tutto ok, a meno di conti sul tasso di faild e unreachable
                 */

                failureRateThreshold = Math.min(failureRateThreshold, 1.0);
                unreachableRateThreshold = Math.min(unreachableRateThreshold, 1.0);

                double failureRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isFailed() ? 1:0), Double::sum) / metrics.size();
                double unreachableRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isUnreachable() ? 1:0), Double::sum) / metrics.size();
                double inactiveRate = failureRate + unreachableRate;

                InstanceMetrics latestMetrics = metrics.get(0);
                if (latestMetrics.isFailed() || unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold || inactiveRate >= 1) { //in ordine di probabilità
                    //Tries to force shutdown the instance
                    adaptationOptions.add(new RemoveInstance(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instances failed or unreachable", true));
                    continue;
                    /*
                    Se l'utlima metrica è failed, allora l'istanza è crashata. Va marcata come istanza spenta (lo deve
                    fare l'executor notificando alla knoledge il set di istanze spente, che verranno marcate come SHUTDOWN) per non
                    confonderla con una potenziale nuova istanza con stesso identificatore.

                    Inoltre, per evitare comportamenti oscillatori, scegliamo di terminare istanze poco reliable che
                    sono spesso unreachable o failed.
                     */
                }

                List<InstanceMetrics> activeMetrics = metrics.stream().filter(InstanceMetrics::isActive).toList(); //la lista contiene almeno un elemento grazie all'inactive rate
                if (activeMetrics.size() < 2) { //non ci sono abbastanza metriche per questa istanza, scelta ottimistica di considerarla come istanza attiva (con parametri medi). Al massimo prima o poi verrà punita.
                    instancesStats.add(new InstanceStats(instance)); // This behaviour is the same adopted when the metric window for the instance is not full
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

                // Single instance statistics (i.e., latest value of the valuesStackHistory that the analysis uses to compute adaptation options)
                InstanceStats instanceStats = new InstanceStats(instance,
                        computeInstanceAvgResponseTime(endpointAvgRespTime),
                        computeInstanceMaxResponseTime(endpointMaxRespTime),
                        computeInstanceAvailability(oldestActiveMetrics, latestActiveMetrics));
                instancesStats.add(instanceStats);
            }

            if (instancesStats.isEmpty()) {
                //TODO appara la newInstanceAvailabilityEstimation, oracolo
                adaptationOptions.add(new AddInstances(service.getServiceId(), service.getCurrentImplementationId(), 1, "No instances available", true));
                log.error("Service {} has no instances with available stats", service.getServiceId());
                continue;
            }

            // Given the adaptation parameters of each service instance, compute the adaptation parameters for the service
            ServiceStats serviceStats = createServiceStatsAndFillJustBornInstances(service, instancesStats);
            // The serviceStats are not available if all the instances of the service are just born.
            // In this case none of the instances have enough metrics to perform the analysis.
            if (serviceStats == null) {
                log.debug("Service {} has no available stats", service.getServiceId());
                continue;
            }
            // Update the adaptation parameters of the service and of its instances ONLY LOCALLY.
            updateServiceAndInstancesWithStats(service, serviceStats, instancesStats);
            log.debug("Service {} -> avail: {}, ART: {}", service.getServiceId(), service.getCurrentValueForParam(Availability.class), service.getCurrentValueForParam(AverageResponseTime.class));
            servicesStatsMap.put(service.getServiceId(), serviceStats);
        }
        return adaptationOptions;
    }

    // Creates and proposes to the knowledge a list of adaptation options for all the services that have filled their
    // analysis window. If the analysis window of a service is filled, the "currentAdaptationParamValue" of the serice
    // is computed for each AdaptationParamSpecification. For each of them, this value is (by this time) the average of
    // the values in the analysis window, and it is used as the reference value for that AdaptationParamSpecification
    // for the service.
    //TODO ho dimenticato qualcosa?
    public List<AdaptationOption> adapt() {
        Set<String> analysedServices = new HashSet<>();
        List<AdaptationOption> proposedAdaptationOptions = new LinkedList<>();
        for (Service service : currentArchitectureMap.values()) {
            proposedAdaptationOptions.addAll(computeAdaptationOptions(service, analysedServices));
        }
        for (AdaptationOption adaptationOption : proposedAdaptationOptions) {
            log.debug("Adaptation option proposed: {}", adaptationOption.getDescription());
        }
        // SEND THE ADAPTATION OPTIONS TO THE KNOWLEDGE FOR THE PLAN
        return proposedAdaptationOptions;
    }

    // Recursive
    private List<AdaptationOption> computeAdaptationOptions(Service service, Set<String> analysedServices) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        ServiceStats serviceStats = servicesStatsMap.get(service.getServiceId());
        if (serviceStats == null || analysedServices.contains(service.getServiceId()))
            // TODO if serviceStats is removed, remove the ==null from the if.
            // Infatti è null solo se le instance sono just born, ma se è così, semplicemente adaptationOptions sarà vuoto perché non si potrà fare adattamento a causa dell'assenza di valori nel valueStack.
            return adaptationOptions;
        analysedServices.add(service.getServiceId()); //must be added here to avoid issues related to circular dependencies
        List<Service> serviceDependencies = service.getDependencies().stream().map(currentArchitectureMap::get).toList();
        for (Service s : serviceDependencies) {
            adaptationOptions.addAll(computeAdaptationOptions(s, analysedServices));
        }

        // Se le dipendenze del servizio corrente hanno problemi non analizzo me stesso ma provo prima a risolvere i problemi delle dipendenze
        // Ergo la lista di adaptation option non contiene adaptation option riguardanti il servizio corrente
        if (!adaptationOptions.isEmpty())
            return adaptationOptions;

        // Analisi del servizio corrente, se non ha dipendenze con problemi
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForParam(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForParam(AverageResponseTime.class, analysisWindowSize);
        if (serviceAvailabilityHistory != null && serviceAvgRespTimeHistory != null) {
            // HERE WE CAN PROPOSE ADAPTATION OPTIONS IF NECESSARY: WE HAVE ANALYSIS_WINDOW_SIZE VALUES FOR THE SERVICE
            // Update the current values for the adaptation parameters of the service and of its instances. Then invalidates the values in the values history
            service.changeCurrentValueForParam(Availability.class, serviceAvailabilityHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            service.changeCurrentValueForParam(AverageResponseTime.class, serviceAvgRespTimeHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            service.invalidateLatestAndPreviousValuesForParam(Availability.class);
            service.invalidateLatestAndPreviousValuesForParam(AverageResponseTime.class);
            service.getInstances().forEach(instance -> {
                instance.changeCurrentValueForParam(Availability.class, instance.getLatestReplicatedAnalysisWindowForParam(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                instance.changeCurrentValueForParam(AverageResponseTime.class, instance.getLatestReplicatedAnalysisWindowForParam(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                instance.invalidateLatestAndPreviousValuesForParam(Availability.class);
                instance.invalidateLatestAndPreviousValuesForParam(AverageResponseTime.class);
            });
            // HERE THE LOGIC FOR CHOOSING THE ADAPTATION OPTIONS TO PROPOSE
            adaptationOptions.addAll(handleAvailabilityAnalysis(service, serviceAvailabilityHistory, serviceStats));
            adaptationOptions.addAll(handleAverageResponseTimeAnalysis(service, serviceAvgRespTimeHistory, serviceStats));
        }
        return adaptationOptions;
    }

    //TODO da rivedere completamente dopo cambio ad analisi media
    private List<AdaptationOption> handleAvailabilityAnalysis(Service service, List<Double> serviceAvailabilityHistory, ServiceStats serviceStats) {//TODO oracolo: togliere avg avail ma usiamo quella dell'oracolo
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        if (!service.getAdaptationParamSpecifications().get(Availability.class).isSatisfied(serviceAvailabilityHistory, parametersSatisfactionRate)) {
            // Order the instances by average availability (ascending)
            List<Instance> instances = service.getInstances().stream()
                    .sorted(Comparator.comparingDouble(i -> i.getLatestReplicatedAnalysisWindowForParam(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow())).toList();
            Instance worstInstance = instances.get(0);
            // 2 adaptation options: add N instances and remove the worst instance. Their benefits will be evaluated by the Plan
            //TODO SE USIAMO L'ORACOLO, NON VA PASSATA LA AVG AVAILABILITY BENSì LA STIMA DELL'AVAILABILITY DELL'ORACOLO
            adaptationOptions.add(new AddInstances(service.getServiceId(), service.getCurrentImplementationId(), serviceStats.getAverageAvailability(), "Add instances to improve the availability of the service"));
            // Ha il senso di "proponi di rimuovere l'istanza con l'availability peggiore. Se il constraint sull'avail continua a essere soddisfatto, hai risparmiato un'istanza"
            // TODO non va qui, perché questa proposta l'analisi deve valutarla se il constraint è soddisfatto, non se non lo è
            //adaptationOptions.add(new RemoveInstance(service.getServiceId(), service.getCurrentImplementationId(), List.of(worstInstance.getInstanceId()), "Remove the least available instance to improve the availability of the service"));
            // TODO mancano le considerazioni sul cambio di implementazione

            //TODO se parriamo a un modello con availability media e non in parallelo la media va calcolata come media pesata sui pesi del LB,
            //TODO e quindi va aggiunta l'opione di adattamento change LB weights anche qui
        }
        return adaptationOptions;
    }

    private List<AdaptationOption> handleAverageResponseTimeAnalysis(Service service, List<Double> serviceAvgRespTimeHistory, ServiceStats serviceStats) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        AverageResponseTime avgRespTimeSpecs = (AverageResponseTime) service.getAdaptationParamSpecifications().get(AverageResponseTime.class);
        if (!avgRespTimeSpecs.isSatisfied(serviceAvgRespTimeHistory, parametersSatisfactionRate)){
            List<Instance> instances = service.getInstances();
            List<Instance> slowInstances = instances.stream().filter(
                    i -> !avgRespTimeSpecs.isSatisfied(
                            i.getLatestReplicatedAnalysisWindowForParam(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow()
                    )
            ).toList();

            //Todo next
            //adaptationOptions.add(new ChangeImplementation(service.getServiceId(), service.getCurrentImplementation(), service.getImplementations().get(0)));

            // If at least one instance satisfies the avg Response time specifications, then we can try to change the LB weights.
            if(slowInstances.size()<instances.size() && service.getConfiguration().getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM)) {
                adaptationOptions.add(new ChangeLoadBalancerWeights(service.getServiceId(), service.getCurrentImplementationId(), serviceStats.getAverageAvailability(), "At least one instance satisfies the avg Response time specifications: change the LB weights"));
            }
            //TODO SE USIAMO L'ORACOLO, NON VA PASSATA LA AVG AVAILABILITY BENSì LA STIMA DELL'AVAILABILITY DELL'ORACOLO (anche sopra nell'if, e va tolto serviceStats da mezzo)
            adaptationOptions.add(new AddInstances(service.getServiceId(), service.getCurrentImplementationId(), serviceStats.getAverageAvailability(), "The service avg response time specification is not satisfied: add instances"));
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

        return (successfulRequests + failedRequests) == 0 ? null : successfulRequests / (successfulRequests + failedRequests);
    }

    private Double computeInstanceMaxResponseTime(Map<String, Double> endpointMaxRespTime) {
        return endpointMaxRespTime.values().stream().max(Double::compareTo).orElse(null);
    }

    private Double computeInstanceAvgResponseTime(Map<String, Double> endpointAvgRespTime) {
        double toReturn =  endpointAvgRespTime.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return toReturn == 0.0 ? null : toReturn;
    }

    // Fill the stats of the instances just born with the average of the other instances stats and create the stats for the service
    // ^OR WITH THE ORACLE TODO.
    // Return null if all the instances are just born
    private ServiceStats createServiceStatsAndFillJustBornInstances(Service service, List<InstanceStats> instancesStats) {
        List<InstanceStats> justBornInstancesStats = new LinkedList<>();
        double averageAvailability, averageMaxResponseTime;
        double availabilityAccumulator = 0;
        double maxResponseTimeAccumulator = 0;
        double averageResponseTime = 0;
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isNewInstance()) {
                justBornInstancesStats.add(instanceStats);
            } else {
                availabilityAccumulator += instanceStats.getAvailability();
                maxResponseTimeAccumulator += instanceStats.getMaxResponseTime();
                averageResponseTime += instanceStats.getAverageResponseTime() * service.getLoadBalancerWeight(instanceStats.getInstance());
            }
        }

        // All the instances are instances just born. So no adaptation is performed
        if (justBornInstancesStats.size() == instancesStats.size())
            return null;

        averageAvailability = availabilityAccumulator / (instancesStats.size() - justBornInstancesStats.size());
        averageMaxResponseTime = maxResponseTimeAccumulator / (instancesStats.size() - justBornInstancesStats.size());
        //TODO due casi per l'avg resp time: se abbiamo l'oracolo, vanno aggiunti semplicementi alla media pesata i valori dell'oracolo per le unavailable stats
        // TODO altrimenti, facciamo la media pesata solo con la somma dei pesi delle istanze considerate (dividendo per un numero <1)

        // Fill the stats of the instances just born with the average of the other instances stats
        for (InstanceStats instanceStats : justBornInstancesStats) { //TODO capire cosa fare con le justBornInstancesStats e l'oracolo
            instanceStats.setAvailability(averageAvailability);
            instanceStats.setMaxResponseTime(averageMaxResponseTime);
            instanceStats.setAverageResponseTime(averageResponseTime);
        }
        Double availability = 1 - instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1 - val)); //TODO cambiare con l'average
        Double maxResponseTime = instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow();
        return new ServiceStats(averageAvailability, averageResponseTime, averageMaxResponseTime, availability, maxResponseTime);
    }

    //Pushes in the respective ValueStackHistory if each instance and services the new values computed from the metrics.
    //The update is performed ONLY LOCALLY.
    private void updateServiceAndInstancesWithStats(Service service, ServiceStats serviceStats, List<InstanceStats> instancesStats) {
        // Update the stats of the instances of the service both locally and remotely
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isNewStats()) {
                AdaptationParamCollection currentInstanceParamCollection = instanceStats.getInstance().getAdaptationParamCollection();
                currentInstanceParamCollection.addNewAdaptationParamValue(Availability.class, instanceStats.getAvailability());
                currentInstanceParamCollection.addNewAdaptationParamValue(MaxResponseTime.class, instanceStats.getMaxResponseTime());
                currentInstanceParamCollection.addNewAdaptationParamValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
                /*
                knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(service.getServiceId(), instanceStats.getInstance().getInstanceId(), Availability.class, instanceStats.getAvailability()));
                knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(service.getServiceId(), instanceStats.getInstance().getInstanceId(), MaxResponseTime.class, instanceStats.getMaxResponseTime()));
                knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createInstanceRequest(service.getServiceId(), instanceStats.getInstance().getInstanceId(), AverageResponseTime.class, instanceStats.getAverageResponseTime()));
                 */
            }
        }
        // Update the stats of the service both locally
        AdaptationParamCollection currentImplementationParamCollection = service.getCurrentImplementation().getAdaptationParamCollection();
        currentImplementationParamCollection.addNewAdaptationParamValue(AverageResponseTime.class, serviceStats.getAverageResponseTime());
        currentImplementationParamCollection.addNewAdaptationParamValue(MaxResponseTime.class, serviceStats.getMaxResponseTime());
        currentImplementationParamCollection.addNewAdaptationParamValue(Availability.class, serviceStats.getAvailability());
        /*
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), AverageResponseTime.class, serviceStats.getAverageResponseTime()));
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), MaxResponseTime.class, serviceStats.getMaxResponseTime()));
        knowledgeClient.addNewAdaptationParameterValue(AddAdaptationParameterValueRequest.createServiceRequest(service.getServiceId(), Availability.class, serviceStats.getAvailability()));
         */

    }

    public void updateAdaptationParamCollectionsInKnowledge(){
        Map<String, Map<String, AdaptationParamCollection>> serviceInstancesNewAdaptationParamCollections = new HashMap<>();
        Map<String, AdaptationParamCollection> serviceNewAdaptationParamCollections = new HashMap<>();
        for(Service service : currentArchitectureMap.values()){
            serviceNewAdaptationParamCollections.put(service.getServiceId(), service.getCurrentImplementation().getAdaptationParamCollection());
            Map<String, AdaptationParamCollection> instanceNewAdaptationParamCollections = new HashMap<>();
            for(Instance instance : service.getInstances()){
                instanceNewAdaptationParamCollections.put(instance.getInstanceId(), instance.getAdaptationParamCollection());
            }
            serviceInstancesNewAdaptationParamCollections.put(service.getServiceId(), instanceNewAdaptationParamCollections);
        }
        knowledgeClient.updateInstancesAdaptationParamCollection(serviceInstancesNewAdaptationParamCollections);
        knowledgeClient.updateServicesAdaptationParamCollection(serviceNewAdaptationParamCollections);
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
