package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.analyse.externalInterfaces.PlanClient;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.metrics.HttpEndpointMetrics;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
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
    private int analysisWindowSize;
    //Number of new metrics to analyse for each instance of each service
    private int metricsWindowSize;
    private double failureRateThreshold;
    private double unreachableRateThreshold;
    private double qosSatisfactionRate;
    private long maxBootTimeSeconds;

    // Variables to temporary store the new values specified by an admin until they are applied during the next loop iteration
    private Integer newMetricsWindowSize;
    private Integer newAnalysisWindowSize;
    private Double newFailureRateThreshold;
    private Double newUnreachableRateThreshold;

    // <serviceId, Service>
    private Map<String, Service> currentArchitectureMap;


    //TODO TEST
    private boolean windowRestaurantPiena = false;

    @Autowired
    private KnowledgeClient knowledgeClient;

    @Autowired
    private PlanClient planClient;

    public AnalyseService(
        @Value("${ANALYSIS_WINDOW_SIZE}") int analysisWindowSize,
        @Value("${METRICS_WINDOW_SIZE}") int metricsWindowSize,
        @Value("${FAILURE_RATE_THRESHOLD}") double failureRateThreshold,
        @Value("${UNREACHABLE_RATE_THRESHOLD}") double unreachableRateThreshold,
        @Value("${QOS_SATISFACTION_RATE}") double qosSatisfactionRate,
        @Value("${MAX_BOOT_TIME_SECONDS}") long maxBootTimeSeconds
    ) {
        if (analysisWindowSize < 1)
            throw new IllegalArgumentException("Analysis window size must be greater than 0");
        if (metricsWindowSize < 3)
            // 3 istanze attive ci garantiscono che ne abbiamo almeno due con un numero di richieste diverse (perché il CB può cambiare spontaneamente solo una volta)
            // Quindi comunque metricsWindowSize>=3
            throw new IllegalArgumentException("Metrics window size must be greater than 2.");
        if (failureRateThreshold < 0 || failureRateThreshold > 1)
            throw new IllegalArgumentException("Failure rate threshold must be between 0 and 1.");
        if (unreachableRateThreshold < 0 || unreachableRateThreshold > 1)
            throw new IllegalArgumentException("Unreachable rate threshold must be between 0 and 1.");
        if (failureRateThreshold + unreachableRateThreshold >= 1)
            throw new IllegalArgumentException("Failure rate threshold + unreachable rate threshold must be less than 1.");
        if (qosSatisfactionRate < 0 || qosSatisfactionRate > 1)
            throw new IllegalArgumentException("Qos satisfaction rate must be between 0 and 1.");
        if (maxBootTimeSeconds < 1)
            throw new IllegalArgumentException("Max boot time seconds must be greater than 0.");
        this.analysisWindowSize = analysisWindowSize;
        this.metricsWindowSize = metricsWindowSize;
        this.failureRateThreshold = failureRateThreshold;
        this.unreachableRateThreshold = unreachableRateThreshold;
        this.qosSatisfactionRate = qosSatisfactionRate;
        this.maxBootTimeSeconds = maxBootTimeSeconds;
    }

    public void startAnalysis() {
        try {
            log.debug("Starting Analyse routine");
            knowledgeClient.notifyModuleStart(Modules.ANALYSE);
            updateWindowAndThresholds(); //update window size and thresholds if they have been changed from an admin
            currentArchitectureMap = knowledgeClient.getServicesMap();
            List<AdaptationOption> forcedAdaptationOptions, proposedAdaptationOptions;
            forcedAdaptationOptions = analyse();
            proposedAdaptationOptions = adapt();
            if (!forcedAdaptationOptions.isEmpty()) {
                log.debug("Forced adaptation options:");
                for (AdaptationOption adaptationOption : forcedAdaptationOptions) {
                    log.debug("|--- {}", adaptationOption.getDescription());
                }
            }
            if (!proposedAdaptationOptions.isEmpty()) {
                log.debug("Proposed adaptation options:");
                for (AdaptationOption adaptationOption : proposedAdaptationOptions) {
                    log.debug("|--- {}", adaptationOption.getDescription());
                }
            }
            //TODO volendo cambiare con la map
            for(Service service : currentArchitectureMap.values()) {
                if(forcedAdaptationOptions.stream().anyMatch(adaptationOption -> adaptationOption.getServiceId().equals(service.getServiceId()))) {
                    invalidateAllQoSHistories(service);
                }
            }
            proposedAdaptationOptions.addAll(forcedAdaptationOptions);
            // SEND THE ADAPTATION OPTIONS TO THE KNOWLEDGE FOR THE PLAN
            knowledgeClient.proposeAdaptationOptions(proposedAdaptationOptions);
            updateQoSCollectionsInKnowledge();
            log.debug("Ending Analyse routine. Notifying the Plan to start the next iteration.\n");
            planClient.start();
        }  catch (Exception e) {
            knowledgeClient.setFailedModule(Modules.ANALYSE);
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error during the Analyse execution: " + e.getMessage());
        }
    }

    // Given the available metrics, creates a new QoS.Value for all the instances when possible, and uses
    // their value to compute each new QoS.Value of the services. It also computes a list of
    // forced Adaptation Options to be applied immediately, as the creation (or removal) of instances upon failures.
    private List<AdaptationOption> analyse() {
        log.debug("\nStarting analysis logic");
        Map<String, AdaptationOption> forcedAdaptationOptions = new HashMap<>();
        for (Service service : currentArchitectureMap.values()) {
            log.debug("Analysing service {}", service.getServiceId());
            boolean existsInstanceWithNewQoSValues = false;
            boolean atLeastOneBootingInstance = false;
            List<InstanceStats> instancesStats = new ArrayList<>();
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                // Ignore shutdown instances (they will disappear from the architecture map in the next iterations)

                if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN) {
                    log.debug("Instance {} is shutdown, ignoring it", instance.getInstanceId());
                    continue;
                    //throw new RuntimeException("Instance " + instance.getInstanceId() + " is in SHUTDOWN status. This should not happen.");
                }
                if (instance.getCurrentStatus() == InstanceStatus.BOOTING) {
                    if ((new Date().getTime() - instance.getLatestInstanceMetricsSnapshot().getTimestamp().getTime()) > maxBootTimeSeconds * 1000) {
                        log.debug("Instance " + instance.getInstanceId() + " is still booting after " + maxBootTimeSeconds + " seconds. Forcing it to shutdown.");
                        forcedAdaptationOptions.put(service.getServiceId(), new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance boot timed out", true));
                    } else {
                        atLeastOneBootingInstance = true;
                    }
                    continue;
                }

                if (instance.getCurrentStatus() == InstanceStatus.FAILED) {
                    log.debug("Instance " + instance.getInstanceId() + " is in FAILED status. Forcing it to shutdown.");
                    forcedAdaptationOptions.put(service.getServiceId(), new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed", true));
                    continue;
                }

                // Get the latest "metricsWindowSize" metrics of the instance collected after the latest adaptation of the service
                List<InstanceMetricsSnapshot> metrics = knowledgeClient.getLatestNMetricsOfCurrentInstance(instance.getServiceId(), instance.getInstanceId(), metricsWindowSize);

                // Not enough data to perform analysis. Can happen only at startup and after an adaptation.
                if (metrics.size() != metricsWindowSize) {
                    instancesStats.add(new InstanceStats(instance));
                    continue;
                }

                double failureRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isFailed() ? 1:0), Double::sum) / metrics.size();
                double unreachableRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isUnreachable() ? 1:0), Double::sum) / metrics.size();
                double inactiveRate = failureRate + unreachableRate;

                if (unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold || inactiveRate >= 1) { //in ordine di probabilità
                    forcedAdaptationOptions.put(service.getServiceId(), new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed or unreachable", true));
                    continue;
                    /*
                    Se l'ultima metrica è failed, allora l'istanza è crashata. Va marcata come istanza spenta (lo farà l'EXECUTE) per non
                    confonderla con una potenziale nuova istanza con stesso identificatore.
                    Inoltre, per evitare comportamenti oscillatori, scegliamo di terminare istanze poco reliable che
                    sono spesso unreachable o failed.

                    se c'è una metrica unreachable abbiamo 4 casi, riassumibili con la seguente regex: UNREACHABLE+ FAILED* ACTIVE*
                    Se c'è una failed di mezzo, la consideriamo come il caso di sotto.
                    Se finisce con active, tutto a posto a meno di anomalie nell'unreachable rate

                    //se c'è una failed ma non una unreachable, abbiamo 2 casi, riassumibili con la seguente regex: FAILED+ ACTIVE*.
                    Caso solo failed: considerare l'istanza come spenta, ignorarla nel calcolo dei QoS.
                    Caso last metric active: tutto ok, a meno di conti sul tasso di faild e unreachable
                    */
                }

                List<InstanceMetricsSnapshot> activeMetrics = metrics.stream().filter(instanceMetricsSnapshot -> instanceMetricsSnapshot.isActive() && instanceMetricsSnapshot.getHttpMetrics().size()>0).toList(); //la lista contiene almeno un elemento grazie all'inactive rate
                if (activeMetrics.size() < 3) {
                    //non ci sono abbastanza metriche per questa istanza, scelta ottimistica di considerarla come buona.
                    // 3 istanze attive ci garantiscono che ne abbiamo due con un numero di richieste diverse
                    instancesStats.add(new InstanceStats(instance));
                } else {
                    InstanceMetricsSnapshot oldestActiveMetrics = activeMetrics.get(activeMetrics.size() - 1);
                    InstanceMetricsSnapshot latestActiveMetrics = activeMetrics.get(0);
                    /* Qui abbiamo almeno 3 metriche attive. Su 3 metriche, almeno due presentano un numero di richieste HTTP diverse
                    (perché il CB può cambiare spontaneamente solo una volta) */
                    instancesStats.add(new InstanceStats(instance, computeInstanceAvgResponseTime(instance, oldestActiveMetrics, latestActiveMetrics), computeInstanceAvailability(oldestActiveMetrics, latestActiveMetrics)));
                    existsInstanceWithNewQoSValues = true;
                }
            }

            if (instancesStats.isEmpty() && !atLeastOneBootingInstance) {
                forcedAdaptationOptions.put(service.getServiceId(), new AddInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), "No instances available", true));
                log.warn("{} has no active or booting instances. Forcing AddInstance option.", service.getServiceId());
                continue;
            }
            if (!existsInstanceWithNewQoSValues) {
                log.warn("{} has no instances with enough metrics to compute new values for the QoSes. Skipping its analysis.", service.getServiceId());
                continue;
            }

            // Given the QoS of each service instance, compute the QoS for the service
            // The stats of the service are not available if all the instances of the service are just born.
            // In this case none of the instances have enough metrics to perform the analysis.
            // Update the QoS of the service and of its instances ONLY LOCALLY.
            updateQoSHistory(service, instancesStats);
            computeServiceAndInstancesCurrentValues(service);
        }
        return forcedAdaptationOptions.values().stream().toList();
    }

    // Creates and proposes to the knowledge a list of adaptation options for all the services that have filled their
    // analysis window. If the analysis window of a service is filled, the "currentQoSValue" of the service
    // is computed for each QoS. For each of them, this value is (by this time) the average of
    // the values in the analysis window, and it is used as the reference value for that QoS
    // for the service.
    private List<AdaptationOption> adapt() {
        log.debug("\nStarting adaptation logic");
        Set<String> analysedServices = new HashSet<>();
        List<AdaptationOption> proposedAdaptationOptions = new LinkedList<>();
        for (Service service : currentArchitectureMap.values()) {
            proposedAdaptationOptions.addAll(computeAdaptationOptions(service, analysedServices));
        }
        return proposedAdaptationOptions;
    }

    private void computeServiceAndInstancesCurrentValues(Service service) {
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForQoS(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize);
        if (serviceAvailabilityHistory != null && serviceAvgRespTimeHistory != null) { // Null if there are not AnalysisWindowSize VALID values in the history
            // Update the current values for the QoS of the service and of its instances. Then invalidates the values in the values history
            service.changeCurrentValueForQoS(Availability.class, serviceAvailabilityHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            service.changeCurrentValueForQoS(AverageResponseTime.class, serviceAvgRespTimeHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            service.getInstances().forEach(instance -> {
                instance.changeCurrentValueForQoS(Availability.class, instance.getLatestFilledAnalysisWindowForQoS(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                instance.changeCurrentValueForQoS(AverageResponseTime.class, instance.getLatestFilledAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            });
            log.debug("{} has a full analysis window. Updating its current values and its instances' current values.", service.getServiceId());
        }
    }

    private void invalidateAllQoSHistories(Service service) {
        service.getInstances().forEach(instance -> {
            instance.invalidateQoSHistory(Availability.class);
            instance.invalidateQoSHistory(AverageResponseTime.class);
        });
        service.invalidateQoSHistory(Availability.class);
        service.invalidateQoSHistory(AverageResponseTime.class);
    }

    /**
     * Computes the adaptation options for a service and the current value of the QoS of the service and its instances.
     * Recursive.
     * @param service: the service to analyse
     * @param analysedServices: the map of services that have already been analysed, to avoid circular dependencies
     * @return the list of adaptation options for the service
     */
    private List<AdaptationOption> computeAdaptationOptions(Service service, Set<String> analysedServices) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        if (analysedServices.contains(service.getServiceId())) // The service has already been analysed, so we can stop the recursion
            return adaptationOptions;
        log.debug("{}: Computing adaptation options", service.getServiceId());
        analysedServices.add(service.getServiceId()); //must be added here to avoid issues related to circular dependencies
        if (!service.getBootingInstances().isEmpty() || !service.getShutdownInstances().isEmpty()) { //We do not perform adaptation if at least one instance of the service is booting or shutdown
            log.warn("{} has at least one instance booting or shutdown. Skipping adaptation for that service.", service.getServiceId());
            return adaptationOptions;
        }
        List<Service> serviceDependencies = service.getDependencies().stream().map(currentArchitectureMap::get).toList();
        for (Service serviceDependency : serviceDependencies) {
            log.debug("{}: Possibly computing adaptation options for dependency {}", service.getServiceId(), serviceDependency.getServiceId());
            adaptationOptions.addAll(computeAdaptationOptions(serviceDependency, analysedServices));
        }

        // Se le dipendenze del servizio corrente hanno problemi non analizzo me stesso ma provo prima a risolvere i problemi delle dipendenze
        // Ergo la lista di adaptation option non contiene adaptation option riguardanti il servizio corrente
        if (!adaptationOptions.isEmpty())
            return adaptationOptions;

        // Analisi del servizio corrente, se non ha dipendenze con problemi
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForQoS(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize);
        if (serviceAvailabilityHistory != null && serviceAvgRespTimeHistory != null) {

            //TODO TEST
            if(service.getServiceId().equalsIgnoreCase("restaurant-service")){
                windowRestaurantPiena = true;
            }

            // Null if there are not AnalysisWindowSize VALID values in the history
            // HERE WE CAN PROPOSE ADAPTATION OPTIONS IF NECESSARY: WE HAVE ANALYSIS_WINDOW_SIZE VALUES FOR THE SERVICE
            log.debug("{}: current Availability value: {} @ {}", service.getServiceId(), service.getCurrentValueForQoS(Availability.class), service.getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(Availability.class).get(analysisWindowSize-1).getTimestamp());
            log.debug("{}: current ART value: {} @ {}", service.getServiceId(), service.getCurrentValueForQoS(AverageResponseTime.class), service.getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(AverageResponseTime.class).get(analysisWindowSize-1).getTimestamp());
            adaptationOptions.addAll(handleAvailabilityAnalysis(service, serviceAvailabilityHistory));
            adaptationOptions.addAll(handleAverageResponseTimeAnalysis(service, serviceAvgRespTimeHistory));
            invalidateAllQoSHistories(service);
        } else {
            log.debug("{} has not a full analysis window. Skipping adaptation for that service.", service.getServiceId());

            //TODO TEST
            if(service.getServiceId().equalsIgnoreCase("restaurant-service")){
                windowRestaurantPiena = false;
            }
        }
        log.debug("{} ending adaptation options computation", service.getServiceId());
        return adaptationOptions;
    }

    private AdaptationOption createChangeImplementationOption(Service service, Class<? extends QoSSpecification> goal) {
        List<String> possibleImplementations = new LinkedList<>();
        for (String possibleImplementationId : service.getPossibleImplementations().keySet())
            if (!possibleImplementationId.equals(service.getCurrentImplementationId()))
                possibleImplementations.add(possibleImplementationId);
        return new ChangeImplementationOption(service.getServiceId(), service.getCurrentImplementationId(), service.getInstances().size(), possibleImplementations, goal, "Changing implementation");
    }

    private List<AdaptationOption> handleAvailabilityAnalysis(Service service, List<Double> serviceAvailabilityHistory) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        Availability availabilitySpecs = (Availability) service.getQoSSpecifications().get(Availability.class);
        if (!availabilitySpecs.isSatisfied(serviceAvailabilityHistory, qosSatisfactionRate)){
            log.debug("{}: Availability is not satisfied at rate {}. Current value: {}. Threshold: {}", service.getServiceId(), qosSatisfactionRate, service.getCurrentValueForQoS(Availability.class), ((Availability) service.getQoSSpecifications().get(Availability.class)).getMinThreshold());
            List<Instance> instances = service.getInstances();
            List<Instance> lessAvailableInstances = instances.stream().filter(
                    i -> !availabilitySpecs.isSatisfied(i.getCurrentValueForQoS(Availability.class).getValue())
            ).toList();

            if(service.shouldConsiderChangingImplementation())
                adaptationOptions.add(createChangeImplementationOption(service, Availability.class));
            // If there is more than one instance and at least one instance satisfies the avg Response time specifications, then we can try to change the LB weights.
            if (instances.size()>1 && lessAvailableInstances.size()<instances.size() && service.getConfiguration().getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM))
                adaptationOptions.add(new ChangeLoadBalancerWeightsOption(service.getServiceId(), service.getCurrentImplementationId(), Availability.class, "At least one instance satisfies the avg Availability specifications"));
            adaptationOptions.add(new AddInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), Availability.class, "The service avg availability specification is not satisfied"));
        }else{
            log.debug("{}: Availability is satisfied at rate {}", service.getServiceId(), qosSatisfactionRate);
        }
        return adaptationOptions;
    }

    private List<AdaptationOption> handleAverageResponseTimeAnalysis(Service service, List<Double> serviceAvgRespTimeHistory) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        AverageResponseTime avgRespTimeSpecs = (AverageResponseTime) service.getQoSSpecifications().get(AverageResponseTime.class);
        if (!avgRespTimeSpecs.isSatisfied(serviceAvgRespTimeHistory, qosSatisfactionRate)){
            log.debug("{}: AVG RT is not satisfied at rate {}. Current value: {}. Threshold: {}", service.getServiceId(), qosSatisfactionRate, service.getCurrentValueForQoS(AverageResponseTime.class), ((AverageResponseTime) service.getQoSSpecifications().get(AverageResponseTime.class)).getMaxThreshold());

            List<Instance> instances = service.getInstances();
            List<Instance> slowInstances = instances.stream().filter(
                    i -> !avgRespTimeSpecs.isSatisfied(i.getCurrentValueForQoS(AverageResponseTime.class).getValue())
            ).toList();

            if(service.shouldConsiderChangingImplementation())
                adaptationOptions.add(createChangeImplementationOption(service, AverageResponseTime.class));
            // If there is more than one instance and at least one instance satisfies the avg Response time specifications, then we can try to change the LB weights.
            if (instances.size()>1 && slowInstances.size()<instances.size() && service.getConfiguration().getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM))
                adaptationOptions.add(new ChangeLoadBalancerWeightsOption(service.getServiceId(), service.getCurrentImplementationId(), AverageResponseTime.class, "At least one instance satisfies the avg Response time specifications"));
            adaptationOptions.add(new AddInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), AverageResponseTime.class, "The service avg response time specification is not satisfied"));
        }
        else{
            log.debug("{}: AVG RT is satisfied at rate {}", service.getServiceId(), qosSatisfactionRate);
        }
        return adaptationOptions;
    }

    private double computeInstanceAvgResponseTime(Instance instance, InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics) {
        double successfulRequestsDuration = 0;
        double successfulRequestsCount = 0;

        for (String endpoint : latestActiveMetrics.getHttpMetrics().keySet()) {
            HttpEndpointMetrics oldestEndpointMetrics = oldestActiveMetrics.getHttpMetrics().get(endpoint);
            successfulRequestsDuration += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDurationOfSuccessful();
            successfulRequestsCount += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
            if(oldestEndpointMetrics != null){
                successfulRequestsDuration -= oldestEndpointMetrics.getTotalDurationOfSuccessful();
                successfulRequestsCount -= oldestEndpointMetrics.getTotalCountOfSuccessful();
            }
        }
        if (successfulRequestsCount == 0) {
            log.warn("{}: No successful requests for instance {}", instance.getServiceId(), instance.getInstanceId());
            return instance.getCurrentValueForQoS(AverageResponseTime.class).getValue();
        }
        return successfulRequestsDuration/successfulRequestsCount;
    }

    private double computeInstanceAvailability(InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics){
        double successfulRequestsCount = 0;
        double totalRequestsCount = 0;

        for(String endpoint : latestActiveMetrics.getHttpMetrics().keySet()){
            HttpEndpointMetrics oldestEndpointMetrics = oldestActiveMetrics.getHttpMetrics().get(endpoint);
            totalRequestsCount += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount();
            successfulRequestsCount += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
            if(oldestEndpointMetrics != null){
                totalRequestsCount -= oldestEndpointMetrics.getTotalCount();
                successfulRequestsCount -= oldestEndpointMetrics.getTotalCountOfSuccessful();
            }
        }
        if (totalRequestsCount == 0)
            throw new RuntimeException("THIS SHOULD NOT HAPPEN");
        return successfulRequestsCount/totalRequestsCount;
    }

    private double computeMaxResponseTime(InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics) {
        double maxRespTime = 0;

        for (String endpoint : oldestActiveMetrics.getHttpMetrics().keySet()) {
            double endpointSuccessfulRequestsCount = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
            if (endpointSuccessfulRequestsCount != 0) {
                maxRespTime = Math.max(maxRespTime, latestActiveMetrics.getHttpMetrics().get(endpoint).getMaxDuration());
            }
        }
        return maxRespTime;
    }

    /** Writes in the instances and service ValueStackHistory the new values computed from the Instances stats built on the metrics window.
     * The update is NOT pushed in the Knowledge.
     *
     * @param service: the service analysed
     * @param instancesStats: InstanceStats list, one for each instance
     */
    private void updateQoSHistory(Service service, List<InstanceStats> instancesStats) {
        double availability = 0;
        double averageResponseTime = 0;
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isFromNewData()) {
                QoSCollection currentInstanceQoSCollection = instanceStats.getInstance().getQoSCollection();
                currentInstanceQoSCollection.addNewQoSValue(Availability.class, instanceStats.getAvailability());
                currentInstanceQoSCollection.addNewQoSValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
            }
            double weight = service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM ?
                    service.getLoadBalancerWeight(instanceStats.getInstance()) : 1.0/instancesStats.size();
            availability += instanceStats.getAvailability() * weight;
            averageResponseTime += instanceStats.getAverageResponseTime() * weight;
        }
        QoSCollection currentImplementationQoSCollection = service.getCurrentImplementation().getQoSCollection();
        currentImplementationQoSCollection.addNewQoSValue(AverageResponseTime.class, averageResponseTime);
        currentImplementationQoSCollection.addNewQoSValue(Availability.class, availability);
    }

    private void updateQoSCollectionsInKnowledge() {
        Map<String, Map<String, QoSCollection>> serviceInstancesNewQoSCollections = new HashMap<>();
        Map<String, QoSCollection> serviceNewQoSCollections = new HashMap<>();
        for(Service service : currentArchitectureMap.values()){
            serviceNewQoSCollections.put(service.getServiceId(), service.getCurrentImplementation().getQoSCollection());
            Map<String, QoSCollection> instanceNewQoSCollections = new HashMap<>();
            for(Instance instance : service.getInstances()){
                instanceNewQoSCollections.put(instance.getInstanceId(), instance.getQoSCollection());
            }
            serviceInstancesNewQoSCollections.put(service.getServiceId(), instanceNewQoSCollections);
        }
        knowledgeClient.updateInstancesQoSCollection(serviceInstancesNewQoSCollections);
        knowledgeClient.updateServicesQoSCollection(serviceNewQoSCollections);
    }

    // Methods to update the Analyse configuration

    public void setNewMetricsWindowSize(Integer newMetricsWindowSize) throws IllegalArgumentException {
        if (newMetricsWindowSize < 3)
            // 3 istanze attive ci garantiscono che ne abbiamo almeno due con un numero di richieste diverse (perché il CB può cambiare spontaneamente solo una volta)
            // Quindi comunque metricsWindowSize>=3
            throw new IllegalArgumentException("Metrics window size must be greater than 2.");
        this.newMetricsWindowSize = newMetricsWindowSize;
    }

    public void setNewAnalysisWindowSize(Integer newAnalysisWindowSize) throws IllegalArgumentException {
        if (newAnalysisWindowSize < 1)
            throw new IllegalArgumentException("Analysis window size must be greater than 0");
        this.newAnalysisWindowSize = newAnalysisWindowSize;
    }

    public void setNewFailureRateThreshold(Double newFailureRateThreshold) throws IllegalArgumentException {
        if (newFailureRateThreshold < 0 || newFailureRateThreshold > 1)
            throw new IllegalArgumentException("Failure rate threshold must be between 0 and 1.");
        this.newFailureRateThreshold = newFailureRateThreshold;
    }

    public void setNewUnreachableRateThreshold(Double newUnreachableRateThreshold) throws IllegalArgumentException {
        if (newUnreachableRateThreshold < 0 || newUnreachableRateThreshold > 1)
            throw new IllegalArgumentException("Unreachable rate threshold must be between 0 and 1.");
        this.newUnreachableRateThreshold = newUnreachableRateThreshold;
    }

    private void updateWindowAndThresholds() {
        if (newMetricsWindowSize != null) {
            this.metricsWindowSize = newMetricsWindowSize;
            log.debug("Metrics window size updated to " + metricsWindowSize);
            newMetricsWindowSize = null;
        }
        if (newFailureRateThreshold != null) {
            failureRateThreshold = newFailureRateThreshold;
            log.debug("Failure rate threshold updated to " + failureRateThreshold);
            newFailureRateThreshold = null;
        }
        if (newUnreachableRateThreshold != null) {
            unreachableRateThreshold = newUnreachableRateThreshold;
            log.debug("Unreachable rate threshold updated to " + unreachableRateThreshold);
            newUnreachableRateThreshold = null;
        }
        if (newAnalysisWindowSize != null) {
            analysisWindowSize = newAnalysisWindowSize;
            log.debug("Analysis window size updated to " + analysisWindowSize);
            newAnalysisWindowSize = null;
        }
    }


    public void breakpoint(){
        log.info("breakpoint");
    }

}
