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
import it.polimi.saefa.knowledge.rest.api.UpdateServiceQosCollectionRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    // <serviceId, List<AdaptOpt>>
    private Map<String, List<AdaptationOption>> servicesForcedAdaptationOptionsMap;
    // <serviceId, List<AdaptOpt>>
    private Map<String, List<AdaptationOption>> servicesProposedAdaptationOptionsMap;

    // Services are in the servicesToSkip set in one of the following cases:
    // - when there is at least a booting instance
    // - when there is at least a shutdown instance
    // - when it has no active or booting instances (in this case we propose an AddInstanceOption)
    // Of those services we compute the new QoS values ONLY of its instances and the only allowed adaptation options are the forced ones.
    // If a service is in the servicesToSkip set, it CAN have (ONLY) FORCED Adaptation Options
    // If the service is skipped, it is because it has problems or has an adaptation in progress
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
            servicesForcedAdaptationOptionsMap = new HashMap<>();
            servicesProposedAdaptationOptionsMap = new HashMap<>();
            analyse();
            adapt();
            for (String serviceId : servicesProposedAdaptationOptionsMap.keySet()) {
                for (AdaptationOption opt : servicesProposedAdaptationOptionsMap.get(serviceId)) {
                    log.debug("|--- {}", opt.getDescription());
                }
            }
            for (String serviceId : servicesForcedAdaptationOptionsMap.keySet()) {
                for (AdaptationOption opt : servicesForcedAdaptationOptionsMap.get(serviceId)) {
                    log.debug("|--- {}", opt.getDescription());
                }
                if (servicesProposedAdaptationOptionsMap.containsKey(serviceId))
                    servicesProposedAdaptationOptionsMap.get(serviceId).addAll(servicesForcedAdaptationOptionsMap.get(serviceId));
                else
                    servicesProposedAdaptationOptionsMap.put(serviceId, servicesForcedAdaptationOptionsMap.get(serviceId));
            }
            // Now servicesProposedAdaptationOptionsMap includes the forced
            // SEND THE ADAPTATION OPTIONS TO THE KNOWLEDGE FOR THE PLAN
            knowledgeClient.proposeAdaptationOptions(servicesProposedAdaptationOptionsMap);
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
    private void analyse() {
        log.debug("\nStarting analysis logic");
        for (Service service : currentArchitectureMap.values()) {
            servicesForcedAdaptationOptionsMap.put(service.getServiceId(), new LinkedList<>());
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
                        servicesForcedAdaptationOptionsMap.get(service.getServiceId()).add(new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance boot timed out", true));
                    } else {
                        log.debug("Instance {} is booting, ignoring it", instance.getInstanceId());
                        atLeastOneBootingInstance = true;
                    }
                    servicesToSkip.add(service.getServiceId());
                    continue;
                }
                if (instance.getCurrentStatus() == InstanceStatus.FAILED) {
                    log.debug("{}: Instance {} is in FAILED status. Forcing it to shutdown.", service.getServiceId(), instance.getInstanceId());
                    servicesForcedAdaptationOptionsMap.get(service.getServiceId()).add(new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed", true));
                    servicesToSkip.add(service.getServiceId());
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
                    log.debug("{}: Rates conditions of instance {} not satisfied.", service.getServiceId(), instance.getInstanceId());
                    servicesForcedAdaptationOptionsMap.get(service.getServiceId()).add(new ShutdownInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed or unreachable", true));
                    servicesToSkip.add(service.getServiceId());
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

                InstanceMetricsSnapshot oldestActiveMetrics = activeMetrics.get(activeMetrics.size() - 1);
                InstanceMetricsSnapshot latestActiveMetrics = activeMetrics.get(0);
                instancesStats.add(new InstanceStats(instance, computeInstanceAvgResponseTime(instance, oldestActiveMetrics, latestActiveMetrics), computeInstanceAvailability(instance, oldestActiveMetrics, latestActiveMetrics)));
                existsInstanceWithNewQoSValues = true;
            }

            if (instancesStats.isEmpty() && !atLeastOneBootingInstance) {
                // Il servizio non è raggiungibile
                log.warn("{}: no active or booting instances. Forcing AddInstance option.", service.getServiceId());
                servicesForcedAdaptationOptionsMap.get(service.getServiceId()).add(new AddInstanceOption(service.getServiceId(), service.getCurrentImplementationId(), "No instances available", true));
                servicesToSkip.add(service.getServiceId());
                continue;
            }

            if (!existsInstanceWithNewQoSValues) {
                // Il servizio non va skippato perché la window potrebbe essere comunque piena. Il controllo verrà fatto dopo
                log.warn("{}: no instances with enough metrics to compute new values for the QoSes. Skipping its analysis.", service.getServiceId());
                continue;
            }

            // Given the stats of each service instance, compute the QoS for the service and for its instances
            // The QoS of the service are not computed if the service is in the set of services to skip
            updateQoSHistory(service, instancesStats, servicesToSkip.contains(service.getServiceId()));

        }
    }

    /** For a given service, it computes the new latest QoS value for its instances and for itself from the InstancesStats (which are built on the metrics window).
     * Then, if there are AnalysisWindowSize VALID values in the history of the service, it computes the new current value for the service and for each instance.
     * The QoS of the service are not computed if the service is in the set of services to skip
     * The update is then pushed in the Knowledge.
     *
     * @param service: the service analysed
     * @param instancesStats: InstanceStats list, one for each instance
     */
    private void updateQoSHistory(Service service, List<InstanceStats> instancesStats, boolean skipServiceQoSComputation) {
        // Logic to compute the new latest value for the service and its instances
        Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesValues = new HashMap<>();
        Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceValues = new HashMap<>();
        double serviceAvailability = 0;
        double serviceAverageResponseTime = 0;
        for (InstanceStats instanceStats : instancesStats) {
            String instanceId = instanceStats.getInstance().getInstanceId();
            if (instanceStats.isFromNewData()) { // only for the instances with a full metrics window
                newInstancesValues.put(instanceId, new HashMap<>());
                QoSCollection currentInstanceQoSCollection = instanceStats.getInstance().getQoSCollection();
                QoSHistory.Value newInstanceValue;
                newInstanceValue = currentInstanceQoSCollection.createNewQoSValue(Availability.class, instanceStats.getAvailability());
                newInstancesValues.get(instanceId).put(Availability.class, newInstanceValue);
                newInstanceValue = currentInstanceQoSCollection.createNewQoSValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
                newInstancesValues.get(instanceId).put(AverageResponseTime.class, newInstanceValue);
            }
            double weight = (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) ?
                    service.getLoadBalancerWeight(instanceStats.getInstance()) : 1.0/instancesStats.size();
            serviceAvailability += instanceStats.getAvailability() * weight;
            serviceAverageResponseTime += instanceStats.getAverageResponseTime() * weight;
        }

        Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesCurrentValues = new HashMap<>();
        Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceCurrentValues = new HashMap<>();

        // Logic for creating the current value of the service and of the instances, only if the service is not to skip
        if (!skipServiceQoSComputation) {
            if (instancesStats.size() != service.getInstances().size())
                throw new RuntimeException("THIS SHOULD NOT HAPPEN");
            QoSCollection currentImplementationQoSCollection = service.getCurrentImplementation().getQoSCollection();
            QoSHistory.Value newServiceValue;
            newServiceValue = currentImplementationQoSCollection.createNewQoSValue(AverageResponseTime.class, serviceAverageResponseTime);
            newServiceValues.put(AverageResponseTime.class, newServiceValue);
            newServiceValue = currentImplementationQoSCollection.createNewQoSValue(Availability.class, serviceAvailability);
            newServiceValues.put(Availability.class, newServiceValue);

            // Logic for creating the current value
            List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForQoS(Availability.class, analysisWindowSize);
            List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize);
            if (serviceAvailabilityHistory != null && serviceAvgRespTimeHistory != null) { // Null if there are not AnalysisWindowSize VALID values in the history
                // If we should not propose adaptation options for the given service, don't update its QoS History (i.e., there are booting or shutdown instances)
                // Update the current values for the QoS of the service.
                QoSHistory.Value newServiceCurrentValue;
                newServiceCurrentValue = service.changeCurrentValueForQoS(Availability.class, serviceAvailabilityHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                newServiceCurrentValues.put(Availability.class, newServiceCurrentValue);
                newServiceCurrentValue = service.changeCurrentValueForQoS(AverageResponseTime.class, serviceAvgRespTimeHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                newServiceCurrentValues.put(AverageResponseTime.class, newServiceCurrentValue);

                service.getInstances().forEach(instance -> {
                    // Update the current values for the QoS of the instances.
                    QoSHistory.Value newInstanceCurrentValue;
                    newInstancesCurrentValues.put(instance.getInstanceId(), new HashMap<>());
                    newInstanceCurrentValue = instance.changeCurrentValueForQoS(Availability.class, instance.getLatestFilledAnalysisWindowForQoS(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                    newInstancesCurrentValues.get(instance.getInstanceId()).put(Availability.class, newInstanceCurrentValue);
                    newInstanceCurrentValue = instance.changeCurrentValueForQoS(AverageResponseTime.class, instance.getLatestFilledAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                    newInstancesCurrentValues.get(instance.getInstanceId()).put(AverageResponseTime.class, newInstanceCurrentValue);
                });

                //todo remove after test
                AtomicBoolean allAvailBelow = new AtomicBoolean(true);
                AtomicBoolean allAvgAbove = new AtomicBoolean(true);

                service.getInstances().forEach(instance -> {
                    if (allAvailBelow.get() && instance.getCurrentValueForQoS(Availability.class).getDoubleValue() >= service.getCurrentValueForQoS(Availability.class).getDoubleValue())
                        allAvailBelow.set(false);
                    if (allAvgAbove.get() && instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue() <= service.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue())
                        allAvgAbove.set(false);
                });

                if (allAvailBelow.get() || allAvgAbove.get()) {
                    throw new RuntimeException("INVESTIGATE");
                }

                log.debug("{} has a full analysis window. Updating its current values and its instances' current values.", service.getServiceId());
            } else {
                log.debug("{} has NOT a full analysis window. Cannot compute a new current value.", service.getServiceId());
            }
        } else {
            log.debug("{}: computation for the new latest QoS value of the service must be skipped", service.getServiceId());
        }

        // Logic for pushing the new values in the Knowledge
        knowledgeClient.updateServiceQosCollection(new UpdateServiceQosCollectionRequest(service.getServiceId(), newInstancesValues, newServiceValues, newInstancesCurrentValues, newServiceCurrentValues));

    }


    // Creates and proposes to the knowledge a list of adaptation options for all the services that have filled their
    // analysis window. If the analysis window of a service is filled, the "currentQoSValue" of the service
    // is computed for each QoS. For each of them, this value is (by this time) the average of
    // the values in the analysis window, and it is used as the reference value for that QoS
    // for the service.
    private void adapt() {
        log.debug("\nStarting adaptation logic");
        // il servizio richiede o sta completando un adattamento (= ha problemi o li sta già risolvendo). Ma non è detto che avrà delle proposte di adattamento (magari ce le ha una dipendenza)
        Map<String, Boolean> servicesRequiringOrCompletingAdaptation = new HashMap<>();
        for (Service service : currentArchitectureMap.values()) {
            computeAdaptationOptions(service, servicesRequiringOrCompletingAdaptation);
        }
    }


    /**
     * Computes the adaptation options for a service.
     * Recursive.
     * @param service the service to analyse
     * @param servicesRequiringOrCompletingAdaptation the map of services that requires adaptation, to avoid circular dependencies. If the service entry is not in the map, it is not analysed yet.
     * @return true if the service has problems and requires adaptation
     */

    /*
    servicesRequiringOrCompletingAdaptation: services already considered for the adaptation proposal logic. If a service is not in the map, it is not analysed yet.
    If the value is true, the service is in transient state (it has been recently adapted) or it requires adaptation.
     */
    private boolean computeAdaptationOptions(Service service, Map<String, Boolean> servicesRequiringOrCompletingAdaptation) {
        String serviceId = service.getServiceId();
        if (servicesRequiringOrCompletingAdaptation.containsKey(serviceId))
            return servicesRequiringOrCompletingAdaptation.get(serviceId); // return info about if the service requires adaptation
        boolean hasForcedOptions = !servicesForcedAdaptationOptionsMap.get(serviceId).isEmpty();
        servicesRequiringOrCompletingAdaptation.put(serviceId, hasForcedOptions); // Start saying that the service requires adaptation if it has forced options
        if (servicesToSkip.contains(serviceId)) { //todo potremmo direttamemte inizializzare la map con quelli toSkip o usare un solo set/map
            log.warn("{}: the analysis decided to skip adaptation for this service.", serviceId);
            servicesRequiringOrCompletingAdaptation.put(serviceId, true); // true because if the service is skipped, it is because it has problems or has an adaptation in progress
            return servicesRequiringOrCompletingAdaptation.get(serviceId);
        }
        List<AdaptationOption> proposedAdaptationOptions = new LinkedList<>();
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForQoS(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForQoS(AverageResponseTime.class, analysisWindowSize);
        if (serviceAvailabilityHistory == null || serviceAvgRespTimeHistory == null) {
            log.warn("{}: the analysis window is not filled yet. Skipping the proposal of Adaptation Options.", serviceId);
            return servicesRequiringOrCompletingAdaptation.get(serviceId);
        }
        log.debug("{}: current Availability value: {} @ {}", service.getServiceId(), service.getCurrentValueForQoS(Availability.class), service.getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(Availability.class).get(analysisWindowSize-1).getTimestamp());
        log.debug("{}: current ART value: {} @ {}", service.getServiceId(), service.getCurrentValueForQoS(AverageResponseTime.class), service.getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(AverageResponseTime.class).get(analysisWindowSize-1).getTimestamp());
        proposedAdaptationOptions.addAll(handleAvailabilityAnalysis(service, serviceAvailabilityHistory));
        proposedAdaptationOptions.addAll(handleAverageResponseTimeAnalysis(service, serviceAvgRespTimeHistory));

        // If there are proposed adaptation options for the service, say that the service requires adaptation.
        // Otherwise, use the previous information
        servicesRequiringOrCompletingAdaptation.put(serviceId, !proposedAdaptationOptions.isEmpty() || servicesRequiringOrCompletingAdaptation.get(serviceId));

        // Dependencies analysis. if a dependency requires adaptation the function returns
        for (Service serviceDependency : service.getDependencies().stream().map(currentArchitectureMap::get).toList()) {
            log.debug("{}: Possibly computing adaptation options for dependency {}", service.getServiceId(), serviceDependency.getServiceId());
            if (computeAdaptationOptions(serviceDependency, servicesRequiringOrCompletingAdaptation)) {
                log.debug("{}: dependency {} has problems. First solving dependency's problems", serviceId, serviceDependency.getServiceId());
                return servicesRequiringOrCompletingAdaptation.get(serviceId);
            }
        }

        if (servicesRequiringOrCompletingAdaptation.get(serviceId)) {
            // Qui il servizio ha sicuro problemi e richiede adattamento per lui (perché le dipendenze non richiedono adattamento)
            if (!proposedAdaptationOptions.isEmpty()) {
                log.debug("{}: no problems for dependencies. Proposing adaptation options", serviceId);
                servicesProposedAdaptationOptionsMap.put(serviceId, proposedAdaptationOptions);
            }
        }
        return servicesRequiringOrCompletingAdaptation.get(serviceId);
    }



    /** For a given service, it invalidates its history of QoSes and its instances' history of QoSes.
     * The update is performed first locally, then the Knowledge is updated.
     * @param service the service considered
     */
    private void invalidateAllQoSHistories(Service service) {
        log.debug("Invalidating all QoS histories for service {}", service.getServiceId());
        service.getInstances().forEach(instance -> {
            instance.invalidateQoSHistory(Availability.class);
            instance.invalidateQoSHistory(AverageResponseTime.class);
        });
        service.invalidateQoSHistory(Availability.class);
        service.invalidateQoSHistory(AverageResponseTime.class);
        knowledgeClient.invalidateQosHistory(service.getServiceId());
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
        if (successfulRequestsCount == 0) {
            log.warn("{}: No successful requests for instance {}. Using its current value for ART", instance.getServiceId(), instance.getInstanceId());
            return instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
        }
        return successfulRequestsDuration/successfulRequestsCount;
    }

    private double computeInstanceAvailability(Instance instance, InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics){
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
        if (totalRequestsCount == 0) {
            log.warn("{}: No successful requests for instance {}. Using its current value for Availability", instance.getServiceId(), instance.getInstanceId());
            return instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
        }
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


/*

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

 */

/*
private List<AdaptationOption> computeAdaptationOptions(Service service) {
        // TODO la analysed services è sbagliata così. Per le dipendenze che sono già state analizzate, bisogna controllare se sono state proposte adapt opt!!!!!
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        if (servicesProposedAdaptationOptionsMap.containsKey(service.getServiceId())) // The service has already been analysed, so we can stop the recursion
            return ;
        log.debug("{}: Computing adaptation options", service.getServiceId());
        servicesProposedAdaptationOptionsMap.put(service.getServiceId(), adaptationOptions); //must be added here to say that the service has been analysed (to avoid issues related to circular dependencies)
        if (servicesToSkip.contains(service.getServiceId())) {
            //We do not compute proposed adaptation options if at least one instance of the service is booting or shutdown, or if it has not analysisWindow VALID values yet
            log.warn("{}: the analysis decided to skip adaptation for this service.", service.getServiceId());
            return false;
        }
        List<Service> serviceDependencies = service.getDependencies().stream().map(currentArchitectureMap::get).toList();
        for (Service serviceDependency : serviceDependencies) {
            log.debug("{}: Possibly computing adaptation options for dependency {}", service.getServiceId(), serviceDependency.getServiceId());
            if (computeAdaptationOptions(serviceDependency)) {
                log.debug("{}: No adaptation options for dependency {}", service.getServiceId(), serviceDependency.getServiceId());

                return adaptationOptions; // empty list
            }
            // TODO fai chiamata rico
            //adaptationOptions.addAll(computeAdaptationOptions(serviceDependency));

        }

        // Se le dipendenze del servizio corrente hanno problemi non analizzo me stesso ma provo prima a risolvere i problemi delle dipendenze
        // Ergo la lista di adaptation option non contiene adaptation option riguardanti il servizio corrente
        if (!adaptationOptions.isEmpty()) {
            invalidateAllQoSHistories(service);
            return adaptationOptions;
        }
        if (servicesProposedAdaptationOptionsMap.)

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
 */
