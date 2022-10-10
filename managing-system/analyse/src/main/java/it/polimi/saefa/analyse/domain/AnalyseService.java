package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.analyse.externalInterfaces.PlanClient;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.options.AddInstance;
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
    private double parametersSatisfactionRate;
    private long maxBootTimeSeconds;

    // Variables to temporary store the new values specified by an admin until they are applied during the next loop iteration
    private Integer newMetricsWindowSize;
    private Integer newAnalysisWindowSize;
    private Double newFailureRateThreshold;
    private Double newUnreachableRateThreshold;

    // <serviceId, Service>
    private Map<String, Service> currentArchitectureMap;


    @Autowired
    private KnowledgeClient knowledgeClient;

    @Autowired
    private PlanClient planClient;

    public AnalyseService(
        @Value("${ANALYSIS_WINDOW_SIZE}") int analysisWindowSize,
        @Value("${METRICS_WINDOW_SIZE}") int metricsWindowSize,
        @Value("${FAILURE_RATE_THRESHOLD}") double failureRateThreshold,
        @Value("${UNREACHABLE_RATE_THRESHOLD}") double unreachableRateThreshold,
        @Value("${PARAMETERS_SATISFACTION_RATE}") double parametersSatisfactionRate,
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
        if (parametersSatisfactionRate < 0 || parametersSatisfactionRate > 1)
            throw new IllegalArgumentException("Parameters satisfaction rate must be between 0 and 1.");
        if (maxBootTimeSeconds < 1)
            throw new IllegalArgumentException("Max boot time seconds must be greater than 0.");
        this.analysisWindowSize = analysisWindowSize;
        this.metricsWindowSize = metricsWindowSize;
        this.failureRateThreshold = failureRateThreshold;
        this.unreachableRateThreshold = unreachableRateThreshold;
        this.parametersSatisfactionRate = parametersSatisfactionRate;
        this.maxBootTimeSeconds = maxBootTimeSeconds;
    }

    public void startAnalysis() {
        try {
            log.debug("Starting analysis");
            knowledgeClient.notifyModuleStart(Modules.ANALYSE);
            updateWindowAndThresholds(); //update window size and thresholds if they have been changed from an admin
            currentArchitectureMap = knowledgeClient.getServicesMap();
            List<AdaptationOption> forcedAdaptationOptions, proposedAdaptationOptions;
            forcedAdaptationOptions = analyse();
            proposedAdaptationOptions = adapt();
            proposedAdaptationOptions.addAll(forcedAdaptationOptions);
            // SEND THE ADAPTATION OPTIONS TO THE KNOWLEDGE FOR THE PLAN
            knowledgeClient.proposeAdaptationOptions(proposedAdaptationOptions);
            updateAdaptationParamCollectionsInKnowledge();
            log.debug("Ending analysis and adaptation. Notifying the Plan to start the next iteration.");
            planClient.start();
        }  catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error during the Analyse execution: " + e.getMessage());
        }
    }

    // Given the available metrics, creates a new AdaptationParameterValue for all the instances when possible, and uses
    // their value to compute each new AdaptationParameterValue of the services. It also computes a list of
    // forced Adaptation Options to be applied immediately, as the creation (or removal) of instances upon failures.
    private List<AdaptationOption> analyse() {
        Collection<Service> currentArchitecture = currentArchitectureMap.values();
        List<AdaptationOption> adaptationOptions = new ArrayList<>();
        for (Service service : currentArchitecture) {
            boolean existsInstanceWithNewMetricsWindow = false;
            List<InstanceStats> instancesStats = new ArrayList<>();
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                // Ignore shutdown instances (they will disappear from the architecture map in the next iterations)

                if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN)
                    throw new RuntimeException("Instance " + instance.getInstanceId() + " is in SHUTDOWN status. This should not happen.");

                if(instance.getCurrentStatus() == InstanceStatus.BOOTING )
                    if((new Date().getTime() - instance.getLatestInstanceMetricsSnapshot().getTimestamp().getTime()) > maxBootTimeSeconds * 1000) {
                        log.debug("Instance " + instance.getInstanceId() + " is still booting after " + maxBootTimeSeconds + " seconds. Forcing it to shutdown.");
                        adaptationOptions.add(new RemoveInstance(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance boot timed out", true));
                        continue;
                    }

                if(instance.getCurrentStatus() == InstanceStatus.FAILED){
                    adaptationOptions.add(new RemoveInstance(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed", true));
                    continue;
                }

                List<InstanceMetricsSnapshot> metrics = new LinkedList<>(knowledgeClient.getLatestNMetricsOfCurrentInstance(instance.getInstanceId(), metricsWindowSize));

                // Not enough data to perform analysis. Can happen only at startup and after an adaptation.
                if (metrics.size() != metricsWindowSize) {
                    if (instance.isJustBorn())
                        // If it's a new instance, the adaptation parameters will have the values provided by the architecture specification.
                        instancesStats.add(new InstanceStats(instance, service.getCurrentImplementation().getAdaptationParamBootBenchmarks()));
                    else
                        // If it's not a new instance, the adaptation parameters will have the latest available value present in the value stack
                        instancesStats.add(new InstanceStats(instance));
                    continue;
                }

                double failureRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isFailed() ? 1:0), Double::sum) / metrics.size();
                double unreachableRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isUnreachable() ? 1:0), Double::sum) / metrics.size();
                double inactiveRate = failureRate + unreachableRate;

                if (unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold || inactiveRate >= 1) { //in ordine di probabilità
                    adaptationOptions.add(new RemoveInstance(service.getServiceId(), service.getCurrentImplementationId(), instance.getInstanceId(), "Instance failed or unreachable", true));
                    continue;
                    /*
                    Se l'utlima metrica è failed, allora l'istanza è crashata. Va marcata come istanza spenta (lo farà l'EXECUTE) per non
                    confonderla con una potenziale nuova istanza con stesso identificatore.
                    Inoltre, per evitare comportamenti oscillatori, scegliamo di terminare istanze poco reliable che
                    sono spesso unreachable o failed.

                    se c'è una metrica unreachable abbiamo 4 casi, riassumibili con la seguente regex: UNREACHABLE+ FAILED* ACTIVE*
                    Se c'è una failed di mezzo, la consideriamo come il caso di sotto.
                    Se finisce con active, tutto a posto a meno di anomalie nell'unreachable rate

                    //se c'è una failed ma non una unreachable, abbiamo 2 casi, riassumibili con la seguente regex: FAILED+ ACTIVE*.
                    Caso solo failed: considerare l'istanza come spenta, ignorarla nel calcolo degli adaptation parameters.
                    Caso last metric active: tutto ok, a meno di conti sul tasso di faild e unreachable
                    */
                }

                List<InstanceMetricsSnapshot> activeMetrics = metrics.stream().filter(InstanceMetricsSnapshot::isActive).toList(); //la lista contiene almeno un elemento grazie all'inactive rate
                if (activeMetrics.size() < 3) {
                    //non ci sono abbastanza metriche per questa istanza, scelta ottimistica di considerarla come buona.
                    // 3 istanze attive ci garantiscono che ne abbiamo due con un numero di richieste diverse
                    if (instance.isJustBorn())
                        // If it's a new instance, the adaptation parameters will have the values provided by the architecture specification.
                        instancesStats.add(new InstanceStats(instance, service.getCurrentImplementation().getAdaptationParamBootBenchmarks()));
                    else
                        // If it's not a new instance, the adaptation parameters will have the latest available value
                        instancesStats.add(new InstanceStats(instance));
                } else {
                    InstanceMetricsSnapshot oldestActiveMetrics = activeMetrics.get(activeMetrics.size() - 1);
                    InstanceMetricsSnapshot latestActiveMetrics = activeMetrics.get(0);

                    /* Qui abbiamo almeno 3 metriche attive. Su 3 metriche, almeno due presentano un numero di richieste HTTP diverse
                    (perché il CB può cambiare spontaneamente solo una volta)
                     */
                    instancesStats.add(new InstanceStats(instance, computeInstanceAvgResponseTime(service, instance, oldestActiveMetrics, latestActiveMetrics), computeMaxResponseTime(oldestActiveMetrics, latestActiveMetrics), computeInstanceAvailability(oldestActiveMetrics, latestActiveMetrics)));
                    existsInstanceWithNewMetricsWindow = true;
                }
            }

            if (instancesStats.isEmpty()) {
                adaptationOptions.add(new AddInstance(service.getServiceId(), service.getCurrentImplementationId(), "No instances available", true));
                log.warn("Service {} has no active instances", service.getServiceId());
                continue;
            }
            if (!existsInstanceWithNewMetricsWindow) {
                log.warn("Service {} has no instances with enough metrics to compute new values for the Adaptation Parameters", service.getServiceId());
                continue;
            }

            // Given the adaptation parameters of each service instance, compute the adaptation parameters for the service
            // The stats of the service are not available if all the instances of the service are just born.
            // In this case none of the instances have enough metrics to perform the analysis.
            // Update the adaptation parameters of the service and of its instances ONLY LOCALLY.
            log.warn("UPDATING SERVICE {} STATS", service.getServiceId());
            updateAdaptationParametersHistory(service, instancesStats);
            //servicesStatsMap.put(service.getServiceId(), serviceStats);
        }
        return adaptationOptions;
    }

    // Creates and proposes to the knowledge a list of adaptation options for all the services that have filled their
    // analysis window. If the analysis window of a service is filled, the "currentAdaptationParamValue" of the serice
    // is computed for each AdaptationParamSpecification. For each of them, this value is (by this time) the average of
    // the values in the analysis window, and it is used as the reference value for that AdaptationParamSpecification
    // for the service.
    private List<AdaptationOption> adapt() {
        Set<String> analysedServices = new HashSet<>();
        List<AdaptationOption> proposedAdaptationOptions = new LinkedList<>();
        for (Service service : currentArchitectureMap.values()) {
            computeServiceAndInstancesCurrentValues(service);
        }

        for(Service service : currentArchitectureMap.values()){
            proposedAdaptationOptions.addAll(computeAdaptationOptions(service, analysedServices));
        }

        for (AdaptationOption adaptationOption : proposedAdaptationOptions) {
            log.debug("Adaptation option proposed: {}", adaptationOption.getDescription());
        }
        return proposedAdaptationOptions;
    }

    private void computeServiceAndInstancesCurrentValues(Service service) {
        List<Double> serviceAvailabilityHistory = service.getLatestAnalysisWindowForParam(Availability.class, analysisWindowSize);
        List<Double> serviceAvgRespTimeHistory = service.getLatestAnalysisWindowForParam(AverageResponseTime.class, analysisWindowSize);
        if (serviceAvailabilityHistory != null && serviceAvgRespTimeHistory != null) { // Null if there are not AnalysisWindowSize VALID values in the history
            // HERE WE CAN PROPOSE ADAPTATION OPTIONS IF NECESSARY: WE HAVE ANALYSIS_WINDOW_SIZE VALUES FOR THE SERVICE
            // Update the current values for the adaptation parameters of the service and of its instances. Then invalidates the values in the values history
            service.changeCurrentValueForParam(Availability.class, serviceAvailabilityHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            service.changeCurrentValueForParam(AverageResponseTime.class, serviceAvgRespTimeHistory.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
            service.invalidateLatestAndPreviousValuesForParam(Availability.class);
            service.invalidateLatestAndPreviousValuesForParam(AverageResponseTime.class);
            service.getInstances().forEach(instance -> {
                if (instance.isJustBorn()) { //If it's just born, set the current value to the one provided in the benchmarks
                    instance.changeCurrentValueForParam(Availability.class, service.getCurrentImplementation().getAdaptationParamBootBenchmarks().get(Availability.class));
                    instance.changeCurrentValueForParam(AverageResponseTime.class, service.getCurrentImplementation().getAdaptationParamBootBenchmarks().get(AverageResponseTime.class));
                } else {
                    instance.changeCurrentValueForParam(Availability.class, instance.getLatestFilledAnalysisWindowForParam(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                    instance.changeCurrentValueForParam(AverageResponseTime.class, instance.getLatestFilledAnalysisWindowForParam(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow());
                    instance.invalidateLatestAndPreviousValuesForParam(Availability.class);
                    instance.invalidateLatestAndPreviousValuesForParam(AverageResponseTime.class);
                }
            });
        }
    }

    /**
     * Computes the adaptation options for a service and the current value of the adaptation parameters of the service and its instances.
     * Recursive.
     * @param service: the service to analyse
     * @param analysedServices: the map of services that have already been analysed, to avoid circular dependencies
     * @return the list of adaptation options for the service
     */
    private List<AdaptationOption> computeAdaptationOptions(Service service, Set<String> analysedServices) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        if (analysedServices.contains(service.getServiceId()))
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
            // Null if there are not AnalysisWindowSize VALID values in the history
            // HERE WE CAN PROPOSE ADAPTATION OPTIONS IF NECESSARY: WE HAVE ANALYSIS_WINDOW_SIZE VALUES FOR THE SERVICE
            log.debug("Service {} -> avail: {}, ART: {}", service.getServiceId(), service.getCurrentValueForParam(Availability.class), service.getCurrentValueForParam(AverageResponseTime.class));
            // HERE THE LOGIC FOR CHOOSING THE ADAPTATION OPTIONS TO PROPOSE
            adaptationOptions.addAll(handleAvailabilityAnalysis(service, serviceAvailabilityHistory));
            adaptationOptions.addAll(handleAverageResponseTimeAnalysis(service, serviceAvgRespTimeHistory));
        }
        return adaptationOptions;
    }

    private List<AdaptationOption> handleAvailabilityAnalysis(Service service, List<Double> serviceAvailabilityHistory) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        Availability availabilitySpecs = (Availability) service.getAdaptationParamSpecifications().get(Availability.class);
        if (!availabilitySpecs.isSatisfied(serviceAvailabilityHistory, parametersSatisfactionRate)){
            List<Instance> instances = service.getInstances();
            List<Instance> lessAvailableInstances = instances.stream().filter(
                    i -> !availabilitySpecs.isSatisfied(
                            i.getLatestFilledAnalysisWindowForParam(Availability.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow()
                    )
            ).toList();

            //adaptationOptions.add(new ChangeImplementation(service.getServiceId(), service.getCurrentImplementation(), service.getImplementations().get(0))); todo

            // If at least one instance satisfies the avg Response time specifications, then we can try to change the LB weights.
            if (lessAvailableInstances.size()<instances.size() && service.getConfiguration().getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM))
                adaptationOptions.add(new ChangeLoadBalancerWeights(service.getServiceId(), service.getCurrentImplementationId(), Availability.class, "At least one instance satisfies the avg Availability specifications: change the LB weights"));
            adaptationOptions.add(new AddInstance(service.getServiceId(), service.getCurrentImplementationId(), Availability.class, "The service avg availability specification is not satisfied: add instances"));
        }
        return adaptationOptions;
    }

    private List<AdaptationOption> handleAverageResponseTimeAnalysis(Service service, List<Double> serviceAvgRespTimeHistory) {
        List<AdaptationOption> adaptationOptions = new LinkedList<>();
        AverageResponseTime avgRespTimeSpecs = (AverageResponseTime) service.getAdaptationParamSpecifications().get(AverageResponseTime.class);
        if (!avgRespTimeSpecs.isSatisfied(serviceAvgRespTimeHistory, parametersSatisfactionRate)){
            List<Instance> instances = service.getInstances();
            List<Instance> slowInstances = instances.stream().filter(
                    i -> !avgRespTimeSpecs.isSatisfied(
                            i.getLatestFilledAnalysisWindowForParam(AverageResponseTime.class, analysisWindowSize).stream().mapToDouble(Double::doubleValue).average().orElseThrow()
                    )
            ).toList();

            //adaptationOptions.add(new ChangeImplementation(service.getServiceId(), service.getCurrentImplementation(), service.getImplementations().get(0))); todo

            // If at least one instance satisfies the avg Response time specifications, then we can try to change the LB weights.
            if (slowInstances.size()<instances.size() && service.getConfiguration().getLoadBalancerType().equals(ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM))
                adaptationOptions.add(new ChangeLoadBalancerWeights(service.getServiceId(), service.getCurrentImplementationId(), AverageResponseTime.class, "At least one instance satisfies the avg Response time specifications: change the LB weights"));
            adaptationOptions.add(new AddInstance(service.getServiceId(), service.getCurrentImplementationId(), AverageResponseTime.class, "The service avg response time specification is not satisfied: add instances"));
        }
        return adaptationOptions;
    }

    private double computeInstanceAvgResponseTime(Service service, Instance instance, InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics) {
        double successfulRequestsDuration = 0;
        double successfulRequestsCount = 0;

        for (String endpoint : oldestActiveMetrics.getHttpMetrics().keySet()) {
            double endpointSuccessfulRequestsCount = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
            if (endpointSuccessfulRequestsCount != 0) {
                double endpointSuccessfulRequestsDuration = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDurationOfSuccessful() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalDurationOfSuccessful();
                successfulRequestsDuration += endpointSuccessfulRequestsDuration;
                successfulRequestsCount += endpointSuccessfulRequestsCount;
            }
        }
        if(successfulRequestsCount == 0) {
            if (instance.isJustBorn())
                return service.getCurrentImplementation().getBootBenchmark(AverageResponseTime.class);
            else
                return instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
        }
        return successfulRequestsDuration/successfulRequestsCount;
    }

    private double computeInstanceAvailability(InstanceMetricsSnapshot oldestActiveMetrics, InstanceMetricsSnapshot latestActiveMetrics){
        double successfulRequestsCount = 0;
        double totalRequestsCount = 0;

        for (String endpoint : oldestActiveMetrics.getHttpMetrics().keySet()) {
            double endpointSuccessfulRequestsCount = latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCountOfSuccessful();
            totalRequestsCount += latestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount() - oldestActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount();
            successfulRequestsCount += endpointSuccessfulRequestsCount;
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

    /** Writes in the instances and service ValueStackHistory the new values computed from the Instances stats built on the metrics window.
     * The update is NOT pushed in the Knowledge.
     *
     * @param service: the service analysed
     * @param instancesStats: InstanceStats list, one for each instance
     */
    private void updateAdaptationParametersHistory(Service service, List<InstanceStats> instancesStats) {
        double availability = 0;
        double maxResponseTimeAccumulator = 0;
        double averageResponseTime = 0;
        double count = 0;
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isFromNewData()) {
                AdaptationParamCollection currentInstanceParamCollection = instanceStats.getInstance().getAdaptationParamCollection();
                currentInstanceParamCollection.addNewAdaptationParamValue(Availability.class, instanceStats.getAvailability());
                currentInstanceParamCollection.addNewAdaptationParamValue(MaxResponseTime.class, instanceStats.getMaxResponseTime());
                currentInstanceParamCollection.addNewAdaptationParamValue(AverageResponseTime.class, instanceStats.getAverageResponseTime());
            }
            double weight = service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM ?
                    service.getLoadBalancerWeight(instanceStats.getInstance()) : 1.0/instancesStats.size();
            availability += instanceStats.getAvailability() * weight;
            maxResponseTimeAccumulator += instanceStats.getMaxResponseTime();
            averageResponseTime += instanceStats.getAverageResponseTime() * weight;
            count++;
        }
        AdaptationParamCollection currentImplementationParamCollection = service.getCurrentImplementation().getAdaptationParamCollection();
        currentImplementationParamCollection.addNewAdaptationParamValue(AverageResponseTime.class, averageResponseTime);
        currentImplementationParamCollection.addNewAdaptationParamValue(MaxResponseTime.class, maxResponseTimeAccumulator / count);
        currentImplementationParamCollection.addNewAdaptationParamValue(Availability.class, availability);
    }

    private void updateAdaptationParamCollectionsInKnowledge() {
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


    public void breakpoint(){
        log.info("breakpoint");
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

    // Fill the stats of the instances just born with the average of the other instances stats and create the stats for the service
    // ^OR WITH THE ORACLE.
    // Return null if all the instances are just born
    private boolean fillJustBornInstances(Service service, List<InstanceStats> instancesStats) {
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
            return false;

        averageAvailability = availabilityAccumulator / (instancesStats.size() - justBornInstancesStats.size());
        averageMaxResponseTime = maxResponseTimeAccumulator / (instancesStats.size() - justBornInstancesStats.size());

        // Fill the stats of the instances just born with the average of the other instances stats
        for (InstanceStats instanceStats : justBornInstancesStats) {
            instanceStats.setAvailability(averageAvailability);
            instanceStats.setMaxResponseTime(averageMaxResponseTime);
            instanceStats.setAverageResponseTime(averageResponseTime);
        }
        return true;//new ServiceStats(averageAvailability, averageResponseTime, averageMaxResponseTime, availability, maxResponseTime);
    }

     */
}
