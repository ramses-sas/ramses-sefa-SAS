package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.analyse.externalInterfaces.PlanClient;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSHistory;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.metrics.HttpEndpointMetrics;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.saefa.knowledge.rest.UpdateServiceQosCollectionRequest;
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

    // when there are booting or shutdown instances or there are not AnalysisWindowSize VALID values saved.
    // of those services we compute the new QoS values of each instance and the only allowed adaptation options are the forced ones.
    // <serviceId>
    private Set<String> servicesToSkip;


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

    // Start the Analyse Module routine
    public void startAnalysis() {
        try {
            log.debug("Starting Analyse routine");
            knowledgeClient.notifyModuleStart(Modules.ANALYSE);
            updateWindowAndThresholds(); //update window size and thresholds if they have been changed from an admin
            currentArchitectureMap = knowledgeClient.getServicesMap();
            servicesToSkip = new HashSet<>();
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
            //TODO volendo cambiare con la map <ServiceId, List<AdaptOpt>>
            // Se la chiave non c'è significa che non c'era una analysis window di quel servizio (e quindi la history non va invalidata)
            // Se la chiave c'è ma la lista è vuota possiamo invalidare ma non c'è bisogno di adattamento
            for (Service service : currentArchitectureMap.values()) {
                if (forcedAdaptationOptions.stream().anyMatch(adaptationOption -> adaptationOption.getServiceId().equals(service.getServiceId()))) {
                    invalidateAllQoSHistories(service);
                }
            }
            proposedAdaptationOptions.addAll(forcedAdaptationOptions);
            // SEND THE ADAPTATION OPTIONS TO THE KNOWLEDGE FOR THE PLAN
            knowledgeClient.proposeAdaptationOptions(proposedAdaptationOptions);
            //updateQoSCollectionsInKnowledge();
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
        List<AdaptationOption> forcedAdaptationOptions = new LinkedList<>();
        for (Service service : currentArchitectureMap.values()) {
            log.debug("Analysing service {}", service.getServiceId());
            boolean existsInstanceWithNewQoSValues = false;
            boolean atLeastOneBootingInstance = false;
            List<InstanceStats> instancesStats = new ArrayList<>();
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN) {
                    // Ignore shutdown instances (they will disappear from the architecture map when no metrics will be received anymore)
                    log.debug("Instance {} is shutdown, ignoring it", instance.getInstanceId());
                    servicesToSkip.add(instance.getServiceId());
                    continue;
                }
                if (instance.getCurrentStatus() == InstanceStatus.BOOTING) {
                    if ((new Date().getTime() - instance.getLatestInstanceMetricsSnapshot().getTimestamp().getTime()) > maxBootTimeSeconds * 1000) {
                        log.debug("Instance " + instance.getInstanceId() + " is still booting after " + maxBootTimeSeconds + " seconds. Forcing it to shutdown.");
                        forcedAdaptationOptions.add(new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance boot timed out", true));
                    } else {
                        log.debug("Instance {} is booting, ignoring it", instance.getInstanceId());
                        atLeastOneBootingInstance = true;
                    }
                    servicesToSkip.add(service.getServiceId());
                    continue;
                }
                if (instance.getCurrentStatus() == InstanceStatus.FAILED) {
                    log.debug("Instance " + instance.getInstanceId() + " is in FAILED status. Forcing it to shutdown.");
                    forcedAdaptationOptions.add(new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed", true));
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
                    forcedAdaptationOptions.add(new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed or unreachable", true));
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
                // Todo andrebbe tolto. Vedi commento nella computeInstanceXXX
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
                forcedAdaptationOptions.add(new AddInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), "No instances available", true));
                log.warn("{} has no active or booting instances. Forcing AddInstance option.", service.getServiceId());
                servicesToSkip.add(service.getServiceId());
                continue;
            }
            if (!existsInstanceWithNewQoSValues) {
                log.warn("{} has no instances with enough metrics to compute new values for the QoSes. Skipping its analysis.", service.getServiceId());
                servicesToSkip.add(service.getServiceId());
                continue;
            }

            // Given the QoS of each service instance, compute the QoS for the service
            // The stats of the service are not available if all the instances of the service are just born.
            // In this case none of the instances have enough metrics to perform the analysis.
            // Update the QoS of the service and of its instances ONLY LOCALLY.

            updateQoSHistory(service, instancesStats);

        }
        return forcedAdaptationOptions;
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
        if (servicesToSkip.contains(service.getServiceId())) { //We do not compute proposed adaptation options if at least one instance of the service is booting or shutdown
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
        if (!adaptationOptions.isEmpty()) {
            invalidateAllQoSHistories(service);
            return adaptationOptions;
        }

        // Analisi del servizio corrente, se non ha dipendenze con problemi
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForQoS(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize);
        // Cannot be null because otherwise they would be inserted in the servicesToSkip Set
        // HERE WE CAN PROPOSE ADAPTATION OPTIONS IF NECESSARY: WE HAVE ANALYSIS_WINDOW_SIZE VALUES FOR THE SERVICE
        log.debug("{}: current Availability value: {} @ {}", service.getServiceId(), service.getCurrentValueForQoS(Availability.class), service.getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(Availability.class).get(analysisWindowSize-1).getTimestamp());
        log.debug("{}: current ART value: {} @ {}", service.getServiceId(), service.getCurrentValueForQoS(AverageResponseTime.class), service.getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(AverageResponseTime.class).get(analysisWindowSize-1).getTimestamp());
        adaptationOptions.addAll(handleAvailabilityAnalysis(service, serviceAvailabilityHistory));
        adaptationOptions.addAll(handleAverageResponseTimeAnalysis(service, serviceAvgRespTimeHistory));
        invalidateAllQoSHistories(service); // Invalidate both if there are adaptation options and if not
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
                    i -> !availabilitySpecs.isSatisfied(i.getCurrentValueForQoS(Availability.class).getDoubleValue())
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
                    i -> !avgRespTimeSpecs.isSatisfied(i.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue())
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



    // Methods to compute the QoS values of an instance from its metrics

    private double computeInstanceAvgResponseTime(Instance instance, InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics) {
        double successfulRequestsDuration = 0;
        double successfulRequestsCount = 0;

        for (String endpoint : latestActiveMetrics.getHttpMetrics().keySet()) {
            successfulRequestsDuration += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDurationOfSuccessful();
            successfulRequestsCount += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
            HttpEndpointMetrics oldestEndpointMetrics = oldestActiveMetrics.getHttpMetrics().get(endpoint);
            if (oldestEndpointMetrics != null) {
                successfulRequestsDuration -= oldestEndpointMetrics.getTotalDurationOfSuccessful();
                successfulRequestsCount -= oldestEndpointMetrics.getTotalCountOfSuccessful();
            }
        }
        // TODO: con questo controllo, che è quello corretto, non ha alcun senso il controllo fatto sopra sull'avere almeno 3 metriche
        if (successfulRequestsCount == 0) {
            log.warn("{}: No successful requests for instance {}", instance.getServiceId(), instance.getInstanceId());
            return instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
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



    // Methods to update the QoS histories

    /** For a given service, it computes the new latest value for its instances and for itself from the Instances stats built on the metrics window.
     * Then, if there are AnalysisWindowSize VALID values in the history of the service, it computes the new current value for the service and for each instance.
     * The update is then pushed in the Knowledge.
     *
     * @param service: the service analysed
     * @param instancesStats: InstanceStats list, one for each instance
     */
    private void updateQoSHistory(Service service, List<InstanceStats> instancesStats) {
        // Logic to compute the new latest value for the service and its instances
        Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesValues = new HashMap<>();
        Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceValues = new HashMap<>();
        double serviceAvailability = 0;
        double serviceAverageResponseTime = 0;
        for (InstanceStats instanceStats : instancesStats) {
            String instanceId = instanceStats.getInstance().getInstanceId();
            if (instanceStats.isFromNewData()) {
                newInstancesValues.put(instanceId, new HashMap<>());
                QoSCollection currentInstanceQoSCollection = instanceStats.getInstance().getQoSCollection();
                QoSHistory.Value newInstanceValue;
                newInstanceValue = currentInstanceQoSCollection.createNewQoSValue(Availability.class, instanceStats.getAvailability());
                newInstancesValues.get(instanceId).put(Availability.class, newInstanceValue);
                newInstanceValue = currentInstanceQoSCollection.createNewQoSValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
                newInstancesValues.get(instanceId).put(AverageResponseTime.class, newInstanceValue);
            }
            // TODO c'è un problema. Se ci sono instanze booting, la somma dei pesi delle istanze non-booting non è 1. Quindi la media del servizio non è corretta.
            // Come risolvere? Non possiamo ridistribuire il peso. Non possiamo darlo tutto all'istanza più forte perché saremmo troppo dipendenti dall'implementazione del LB nel managed
            double weight = service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM ?
                    service.getLoadBalancerWeight(instanceStats.getInstance()) : 1.0/instancesStats.size();
            serviceAvailability += instanceStats.getAvailability() * weight;
            serviceAverageResponseTime += instanceStats.getAverageResponseTime() * weight;
        }
        // If we should not propose adaptation options for the given service, don't update its QoS History (i.e., there are booting or shutdown instances)
        if (!servicesToSkip.contains(service.getServiceId())) {
            QoSCollection currentImplementationQoSCollection = service.getCurrentImplementation().getQoSCollection();
            QoSHistory.Value newServiceValue;
            newServiceValue = currentImplementationQoSCollection.createNewQoSValue(AverageResponseTime.class, serviceAverageResponseTime);
            newServiceValues.put(AverageResponseTime.class, newServiceValue);
            newServiceValue = currentImplementationQoSCollection.createNewQoSValue(Availability.class, serviceAvailability);
            newServiceValues.put(Availability.class, newServiceValue);
        }

        // Logic for creating the current value
        Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesCurrentValues = new HashMap<>();
        Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceCurrentValues = new HashMap<>();
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForQoS(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize);
        if (serviceAvailabilityHistory != null && serviceAvgRespTimeHistory != null) { // Null if there are not AnalysisWindowSize VALID values in the history
            // If we should not propose adaptation options for the given service, don't update its QoS History (i.e., there are booting or shutdown instances)
            if (!servicesToSkip.contains(service.getServiceId())) {
                // Update the current values for the QoS of the service.
                QoSHistory.Value newServiceCurrentValue;
                newServiceCurrentValue = service.changeCurrentValueForQoS(Availability.class, serviceAvailabilityHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                newServiceCurrentValues.put(Availability.class, newServiceCurrentValue);
                newServiceCurrentValue = service.changeCurrentValueForQoS(AverageResponseTime.class, serviceAvgRespTimeHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                newServiceCurrentValues.put(AverageResponseTime.class, newServiceCurrentValue);
            }
            service.getInstances().forEach(instance -> {
                // Update the current values for the QoS of the instances.
                QoSHistory.Value newInstanceCurrentValue;
                newInstancesCurrentValues.put(instance.getInstanceId(), new HashMap<>());
                newInstanceCurrentValue = instance.changeCurrentValueForQoS(Availability.class, instance.getLatestFilledAnalysisWindowForQoS(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                newInstancesCurrentValues.get(instance.getInstanceId()).put(Availability.class, newInstanceCurrentValue);
                newInstanceCurrentValue = instance.changeCurrentValueForQoS(AverageResponseTime.class, instance.getLatestFilledAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                newInstancesCurrentValues.get(instance.getInstanceId()).put(AverageResponseTime.class, newInstanceCurrentValue);
            });
            log.debug("{} has a full analysis window. Updating its current values and its instances' current values.", service.getServiceId());
        } else {
            // Skip the service if there are not AnalysisWindowSize VALID values saved.
            log.debug("{} has not a full analysis window. Skipping adaptation for that service.", service.getServiceId());
            servicesToSkip.add(service.getServiceId());
        }

        // Logic for pushing the new values in the Knowledge
        knowledgeClient.updateServiceQosCollection(new UpdateServiceQosCollectionRequest(service.getServiceId(), newInstancesValues, newServiceValues, newInstancesCurrentValues, newServiceCurrentValues));

    }

    /** For a given service, it invalidates its history of QoSes and its instances' history of QoSes.
     * The update is performed first locally, then the Knowledge is updated.
     * @param service the service considered
     */
    private void invalidateAllQoSHistories(Service service) {
        service.getInstances().forEach(instance -> {
            instance.invalidateQoSHistory(Availability.class);
            instance.invalidateQoSHistory(AverageResponseTime.class);
        });
        service.invalidateQoSHistory(Availability.class);
        service.invalidateQoSHistory(AverageResponseTime.class);
        knowledgeClient.invalidateQosHistory(service.getServiceId());
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



    /* Quando passavamo l'intera QoS collection alla knowledge. Prima della QoSrepo
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
     */
}
