package it.polimi.ramses.plan.domain;

import com.google.ortools.linearsolver.*;
import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.adaptation.options.*;
import it.polimi.ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.ramses.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.ramses.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.ramses.knowledge.domain.architecture.*;
import it.polimi.ramses.plan.externalInterfaces.ExecuteClient;
import it.polimi.ramses.plan.externalInterfaces.KnowledgeClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class PlanService {

    @Autowired
    private KnowledgeClient knowledgeClient;

    @Autowired
    private ExecuteClient executeClient;

    @Getter
    @Setter
    private boolean adaptationAuthorized = false;

    // For a given service, the system must not be in a transition state.
    // In that case, only forced adaptation options are allowed.
    public void startPlan() {
        try {
            log.info("\nStarting plan");
            knowledgeClient.notifyModuleStart(Modules.PLAN);
            Map<String, Service> servicesMap = knowledgeClient.getServicesMap();
            Map<String, List<AdaptationOption>> proposedAdaptationOptions = knowledgeClient.getProposedAdaptationOptions();
            Map<String, List<AdaptationOption>> chosenAdaptationOptions = new HashMap<>();

            if (adaptationAuthorized) {
                proposedAdaptationOptions.forEach((serviceId, options) -> {
                    log.debug("Analysing service: {}", serviceId);
                    List<AdaptationOption> chosenAdaptationOptionList = new LinkedList<>();
                    // Initialized with all the forced options
                    List<AdaptationOption> forcedAdaptationOptions = new LinkedList<>(options.stream().filter(AdaptationOption::isForced).toList());

                    if (forcedAdaptationOptions.isEmpty()) {
                        log.debug("{} has no forced options. Analysing proposed adaptation options.", serviceId);
                        for (AdaptationOption option : options) {
                            log.debug("Proposed option: {}", option.getDescription());
                            if (option.getClass().equals(ChangeLoadBalancerWeightsOption.class)) {
                                ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) option;
                                Map<String, Double> newWeights = handleChangeLoadBalancerWeights(servicesMap.get(option.getServiceId()));
                                if (newWeights != null) { // If it's null it means that the problem has no solution
                                    List<String> instancesToShutdownIds = new LinkedList<>();
                                    newWeights.forEach((instanceId, weight) -> {
                                        if (weight == 0.0) {
                                            instancesToShutdownIds.add(instanceId);
                                        }
                                    });
                                    instancesToShutdownIds.forEach(newWeights::remove);
                                    changeLoadBalancerWeightsOption.setNewWeights(newWeights);
                                    changeLoadBalancerWeightsOption.setInstancesToShutdownIds(instancesToShutdownIds);
                                }
                            }
                            if (option.getClass().equals(AddInstanceOption.class))
                                handleAddInstance((AddInstanceOption) option, servicesMap.get(option.getServiceId()));
                            if (option.getClass().equals(ShutdownInstanceOption.class))
                                handleShutdownInstance((ShutdownInstanceOption) option, servicesMap.get(option.getServiceId()), false);
                            if (option.getClass().equals(ChangeImplementationOption.class))
                                handleChangeImplementation((ChangeImplementationOption) option, servicesMap.get(option.getServiceId()));
                        }

                        AdaptationOption chosenOption = extractBestOption(servicesMap.get(serviceId), proposedAdaptationOptions.get(serviceId).stream().filter(option -> !option.getClass().equals(ChangeLoadBalancerWeightsOption.class) || ((ChangeLoadBalancerWeightsOption)option).getNewWeights()!=null).collect(Collectors.toList()));
                        if (chosenOption != null)
                            chosenAdaptationOptionList.add(chosenOption);
                    }
                    else {
                        // If there is at least a forced option, all the other options are ignored
                        log.debug("{} has forced Adaptation options", serviceId);
                        //We first perform all the ShutdownInstanceOptions and then perform the final AddInstanceOption, if any. This same order must be respected by the Execute.
                        for (AdaptationOption option : forcedAdaptationOptions.stream().filter(option -> option.getClass().equals(ShutdownInstanceOption.class)).toList()) {
                            log.debug(option.toString());
                            chosenAdaptationOptionList.add(handleShutdownInstance((ShutdownInstanceOption) option, servicesMap.get(option.getServiceId()), true));
                        }
                        List<AddInstanceOption> addInstanceOptions = forcedAdaptationOptions.stream().filter(option -> option.getClass().equals(AddInstanceOption.class)).map(option -> (AddInstanceOption) option).toList();
                        if (addInstanceOptions.size() > 1) {
                            log.error("More than one add instance option is forced");
                            throw new RuntimeException("More than one add instance option is forced");
                        } else if (addInstanceOptions.size() == 1) {
                            chosenAdaptationOptionList.add(handleAddInstance(addInstanceOptions.get(0), servicesMap.get(addInstanceOptions.get(0).getServiceId())));
                            log.debug(addInstanceOptions.get(0).toString());
                        }
                    }
                    if (!chosenAdaptationOptionList.isEmpty())
                        chosenAdaptationOptions.put(serviceId, chosenAdaptationOptionList);
                });
                Set<String> servicesAlreadyProcessed = new HashSet<>();
                servicesMap.forEach((serviceId, service) -> {
                    if (service.isInTransitionState() || chosenAdaptationOptions.containsKey(serviceId)) {
                        invalidateQoSHistoryOfServiceAndDependants(servicesMap, serviceId, servicesAlreadyProcessed);
                    }
                });
                knowledgeClient.chooseAdaptationOptions(chosenAdaptationOptions);
            }
            log.info("Ending plan. Notifying the Execute module to start the next iteration.");
            executeClient.start();
        } catch (Exception e) {
            knowledgeClient.setFailedModule(Modules.PLAN);
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error during the plan execution", e);
        }
    }

    private void invalidateQoSHistoryOfServiceAndDependants(Map<String, Service> servicesMap, String serviceId, Set<String> servicesAlreadyProcessed) {
        if (servicesAlreadyProcessed.contains(serviceId))
            return;
        servicesAlreadyProcessed.add(serviceId);
        Service service = servicesMap.get(serviceId);
        log.debug("{}: invalidating QoS history", serviceId);
        invalidateAllQoSHistories(service);
        servicesMap.values().forEach(s -> {
            if (s.getDependencies().contains(serviceId))
                invalidateQoSHistoryOfServiceAndDependants(servicesMap, s.getServiceId(), servicesAlreadyProcessed);
        });
    }


    /** For a given service, it invalidates its history of QoSes and its instances' history of QoSes.
     * The update is performed first locally, then the Knowledge is updated.
     * @param service the service considered
     */
    private void invalidateAllQoSHistories(Service service) {
        log.debug("Invalidating all QoS histories for service {}", service.getServiceId());
        knowledgeClient.invalidateQosHistory(service.getServiceId());
    }

    private ShutdownInstanceOption handleShutdownInstance(ShutdownInstanceOption shutdownInstanceOption, Service service, boolean isForced) {
        if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
            shutdownInstanceOption.setNewWeights(redistributeWeight(service.getLoadBalancerWeights(), List.of(shutdownInstanceOption.getInstanceToShutdownId())));
            if (isForced) {
                service.setLoadBalancerWeights(shutdownInstanceOption.getNewWeights());
                service.removeInstance(service.getInstancesMap().get(shutdownInstanceOption.getInstanceToShutdownId()));
            }
        }
        return shutdownInstanceOption;
    }

    // Returns the new weights after redistributing the weight of the instances to shutdown. The instances shutdown are removed from the map. They can be retrieved computing the key set difference.
    private Map<String, Double> recursivelyRemoveInstancesUnderThreshold(Map<String, Double> originalWeights, double threshold) {
        List<String> instancesToShutdownIds = originalWeights.entrySet().stream().filter(entry -> entry.getValue() < threshold).map(Map.Entry::getKey).toList();
        if (instancesToShutdownIds.isEmpty()) // Stop when no instances are under the threshold
            return originalWeights;
        double newThreshold = threshold * originalWeights.size() / (originalWeights.size() - instancesToShutdownIds.size());
        Map<String, Double> newWeights = redistributeWeight(originalWeights, instancesToShutdownIds);
        return recursivelyRemoveInstancesUnderThreshold(newWeights, newThreshold);
    }

    // We assume that only one AddInstance option per service for each loop iteration is proposed by the Analyse module.
    private AddInstanceOption handleAddInstance(AddInstanceOption addInstanceOption, Service service) {
        if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
            double shutdownThreshold = service.getCurrentImplementation().getInstanceLoadShutdownThreshold() / (service.getInstances().size()+1);
            Map<String, Double> weightsRedistributed = reduceWeightsForNewInstance(service.getLoadBalancerWeights(), 1);
            weightsRedistributed.put("NEWINSTANCE", 1/(double)(weightsRedistributed.size()+1));
            weightsRedistributed = recursivelyRemoveInstancesUnderThreshold(weightsRedistributed, shutdownThreshold);
            addInstanceOption.setNewInstanceWeight(weightsRedistributed.remove("NEWINSTANCE"));
            addInstanceOption.setOldInstancesNewWeights(weightsRedistributed);
            Map<String, Double> finalWeightsRedistributed = weightsRedistributed;
            addInstanceOption.setInstancesToShutdownIds(service.getLoadBalancerWeights().keySet().stream().filter(id -> !finalWeightsRedistributed.containsKey(id)).toList());
            if (!addInstanceOption.getInstancesToShutdownIds().isEmpty()) {
                log.warn("The Analyse module proposed to add an instance to service {} but also to shutdown some instances.", service.getServiceId());
                log.warn("New Instance weight: {}", addInstanceOption.getNewInstanceWeight());
                log.warn("Old instances new weights: {}", addInstanceOption.getOldInstancesNewWeights());
                log.warn("Instances to shutdown: {}", addInstanceOption.getInstancesToShutdownIds());
            }
        }
        return addInstanceOption;
    }

    public Map<String, Double> handleChangeLoadBalancerWeights(Service service) {
        Map<String, Double> previousWeights = service.getLoadBalancerWeights();
        double shutdownThreshold = service.getCurrentImplementation().getInstanceLoadShutdownThreshold() / service.getInstances().size();
        double defaultWeight = 1.0 / service.getInstances().size();
        boolean emptyWeights = previousWeights.isEmpty();

        Map<String, Double> newWeights = new HashMap<>();

        MPSolver solver = MPSolver.createSolver("SCIP");
        Map<String, MPVariable> weightsVariables = new HashMap<>();
        Map<String, MPVariable> activationsVariables = new HashMap<>();
        MPObjective objective = solver.objective();// min{∑(w_i/z_i) - ∑(a_i * z_i)}

        double serviceAvgRespTime = service.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
        double serviceAvgAvailability = service.getCurrentValueForQoS(Availability.class).getDoubleValue();
        double k_s = serviceAvgAvailability / serviceAvgRespTime; // service performance indicator
        MPConstraint sumOfWeights = solver.makeConstraint(1.0, 1.0, "sumOfWeights"); // ∑(w_i) = 1

        for (Instance instance : service.getInstances()) {
            MPVariable weight = solver.makeNumVar(0, 1, instance.getInstanceId() + "_weight");
            MPVariable activation = solver.makeIntVar(0, 1, instance.getInstanceId() + "_activation");
            weightsVariables.put(instance.getInstanceId(), weight);
            activationsVariables.put(instance.getInstanceId(), activation);
            sumOfWeights.setCoefficient(weight, 1);

            // w_i - a_i*shutdownThreshold >= 0 <=>
            // w_i >= a_i * shutdownThreshold
            MPConstraint lowerBoundConstraint = solver.makeConstraint(0, Double.POSITIVE_INFINITY, instance.getInstanceId() + "_activation_lowerBoundConstraint");
            lowerBoundConstraint.setCoefficient(weight, 1);
            lowerBoundConstraint.setCoefficient(activation, -shutdownThreshold);

            // w_i - a_i<=0 <=>
            // w_i <= a_i
            MPConstraint upperBoundConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0, instance.getInstanceId() + "_activation_upperBoundConstraint");
            upperBoundConstraint.setCoefficient(weight, 1);
            upperBoundConstraint.setCoefficient(activation, -1);

            if (emptyWeights)
                previousWeights.put(instance.getInstanceId(), defaultWeight);

            double instanceAvgRespTime = instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
            double instanceAvailability = instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
            double k_i = instanceAvailability / instanceAvgRespTime;
            double z_i = k_i / k_s;

            if (k_i != 0.0) {
                objective.setCoefficient(weight, 1 / z_i);
                objective.setCoefficient(activation, -z_i);
            }else{
                MPConstraint forceZeroWeight = solver.makeConstraint(0, 0, instance.getInstanceId() + "_forceZeroWeight");
                forceZeroWeight.setCoefficient(activation, 1);
            }
        }

        for (Instance instance_i : service.getInstances()) {
            MPVariable weight_i = weightsVariables.get(instance_i.getInstanceId());
            double instanceAvgRespTime_i = instance_i.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
            double instanceAvailability_i = instance_i.getCurrentValueForQoS(Availability.class).getDoubleValue();
            double k_i = instanceAvailability_i / instanceAvgRespTime_i;
            double z_i = k_i / k_s;

            if (k_i == 0) {
                continue;
            }

            MPConstraint growthConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, z_i, instance_i.getInstanceId() + "constraint5"); // w_i<= z_i * [P_i + ∑(P_j * (1-a_j))] <=> w_i + z_i*∑ P_j * a_j <=z_i
            growthConstraint.setCoefficient(weight_i, 1);

            for (Instance instance_j : service.getInstances()) {
                if(instance_i.equals(instance_j))
                    continue;
                MPVariable weight_j = weightsVariables.get(instance_j.getInstanceId());
                double instanceAvgRespTime_j = instance_j.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
                double instanceAvailability_j = instance_j.getCurrentValueForQoS(Availability.class).getDoubleValue();
                growthConstraint.setCoefficient(activationsVariables.get(instance_j.getInstanceId()), z_i * previousWeights.get(instance_j.getInstanceId()));

                double k_j = instanceAvailability_j / instanceAvgRespTime_j;
                double k_ij = k_i / k_j;

                if (k_i >= k_j) {
                    // w_i - k_i/k_j * w_j + a_j  <= 1 <=>
                    // w_i <= k_i/k_j * w_j + (1 - a_j)
                    MPConstraint weightsBalancingConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1, instance_i + "constraint4");
                    weightsBalancingConstraint.setCoefficient(weight_i, 1);
                    weightsBalancingConstraint.setCoefficient(weight_j, -k_ij);
                    weightsBalancingConstraint.setCoefficient(activationsVariables.get(instance_j.getInstanceId()), 1);
                }
            }
        }

        objective.setMinimization();
        final MPSolver.ResultStatus resultStatus = solver.solve();

        StringBuilder sb = new StringBuilder();
        sb.append("\n Minimization problem for service ").append(service.getServiceId()).append(" solved with status ").append(resultStatus);
        sb.append("\nShutdown threshold: ").append(shutdownThreshold).append("\n");
        sb.append("Service response time: ").append(String.format("%.2f", serviceAvgRespTime)).append("ms\n");
        sb.append("Service availability: ").append(String.format("%.2f", serviceAvgAvailability)).append("\n");
        sb.append("Service k_s: ").append(String.format("%.2e", k_s)).append("\n");
        sb.append("\nSolution: \n");
        sb.append("Objective value = ").append(objective.value()).append("\n");

        for (String instanceId : weightsVariables.keySet()) {
            String P_i = String.format("%.2f", previousWeights.get(instanceId));
            double avail_i_double = service.getInstance(instanceId).getCurrentValueForQoS(Availability.class).getDoubleValue();
            String avail_i = String.format("%.2f", avail_i_double);
            double ART_i_double = service.getInstance(instanceId).getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
            String ART_i = String.format("%.2f", ART_i_double);
            double k_i_double = avail_i_double/ART_i_double;
            String k_i = String.format("%.2e", k_i_double);
            double z_i_double = k_i_double/k_s;
            String z_i = String.format("%.2e", z_i_double);
            sb.append(instanceId + " { P_i="+P_i+", k_i="+k_i+", z_i="+z_i+", ART_i="+ART_i+"ms, avail_i="+avail_i+" }\n");
        }

        if (resultStatus != MPSolver.ResultStatus.OPTIMAL && resultStatus != MPSolver.ResultStatus.FEASIBLE && resultStatus != MPSolver.ResultStatus.UNBOUNDED) {
            log.error("The problem of determining new weights for service " + service.getServiceId() + " does not have an optimal solution!");
            log.debug(sb.toString());
            log.debug(solver.exportModelAsLpFormat());
            return null;
        }

        if(previousWeights.size() != weightsVariables.size()){
            throw new RuntimeException("The number of weights is not the same as the number of instances!");
        }

        for (String instanceId : weightsVariables.keySet()) {
            if(weightsVariables.get(instanceId) == null)
                throw new RuntimeException("Instance " + instanceId + " not found in the weights map");
            double W_i_double = weightsVariables.get(instanceId).solutionValue();
            String W_i = String.format("%.3f", W_i_double);
            newWeights.put(instanceId, W_i_double);
            sb.append(instanceId + " { W_i="+W_i+" }\n");
        }

        log.debug(sb.toString());

        return newWeights;
    }

    public ChangeImplementationOption handleChangeImplementation(ChangeImplementationOption changeImplementationOption, Service service){
        String bestImplementationId = null;
        double bestImplementationBenefit = 0;
        for (String implementationId: changeImplementationOption.getPossibleImplementations()) {
            Class<? extends QoSSpecification> goal = changeImplementationOption.getQosGoal();
            ServiceImplementation implementation = service.getPossibleImplementations().get(implementationId);
            double benchmark = implementation.getBenchmark(changeImplementationOption.getQosGoal());
            if (Availability.class == goal) {
                benchmark = benchmark * implementation.getPreference();
                if (bestImplementationId == null) {
                    bestImplementationId = implementationId;
                    bestImplementationBenefit = benchmark;
                }
                if (benchmark > bestImplementationBenefit) {
                    bestImplementationId = implementationId;
                    bestImplementationBenefit = benchmark;
                }
            } else if(AverageResponseTime.class == goal) {
                benchmark = benchmark / implementation.getPreference();
                if (bestImplementationId == null) {
                    bestImplementationId = implementationId;
                    bestImplementationBenefit = benchmark;
                }
                if (benchmark < bestImplementationBenefit) {
                    bestImplementationId = implementationId;
                    bestImplementationBenefit = benchmark;
                }
            }
        }
        changeImplementationOption.setNewImplementationId(bestImplementationId);

        return changeImplementationOption;
    }

    //Right now, this function is called only if there are no forced adaptation options
    public AdaptationOption extractBestOption(Service service, List<AdaptationOption> toCompare) {
        if (toCompare.size() == 0)
            return null;
        Map<Class<? extends QoSSpecification>, Double> benefits = new HashMap<>();
        Map<Class<? extends QoSSpecification>, AdaptationOption> bestOptionForGoal = new HashMap<>();
        log.debug("{}: Extracting best option from {} options", service.getServiceId(), toCompare.size());

        for (AdaptationOption adaptationOption : toCompare) {
            List<Instance> instances = service.getInstances();
            if (adaptationOption.getQosGoal() == Availability.class) {
                double availabilityEstimation = 0.0;
                if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
                    if (ChangeLoadBalancerWeightsOption.class.equals(adaptationOption.getClass())) {
                        ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!changeLoadBalancerWeightsOption.getInstancesToShutdownIds().contains(instance.getInstanceId()))
                                availabilityEstimation += changeLoadBalancerWeightsOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
                        }
                    }
                    else if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        AddInstanceOption addInstanceOption = (AddInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!addInstanceOption.getInstancesToShutdownIds().contains(instance.getInstanceId()))
                                availabilityEstimation += addInstanceOption.getOldInstancesNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
                        }
                        availabilityEstimation += addInstanceOption.getNewInstanceWeight() * service.getCurrentImplementation().getBenchmark(Availability.class);
                    } else if (ShutdownInstanceOption.class.equals(adaptationOption.getClass())) {
                        ShutdownInstanceOption shutdownInstanceOption = (ShutdownInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(shutdownInstanceOption.getInstanceToShutdownId()))
                                availabilityEstimation += shutdownInstanceOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
                        }
                    }
                }
                else {
                    if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        for (Instance instance : instances) {
                            availabilityEstimation += instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
                        }
                        availabilityEstimation += service.getCurrentImplementation().getBenchmark(Availability.class);
                        availabilityEstimation /= instances.size() + 1;
                    } else if (ShutdownInstanceOption.class.equals(adaptationOption.getClass())) {
                        ShutdownInstanceOption shutdownInstanceOption = (ShutdownInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(shutdownInstanceOption.getInstanceToShutdownId()))
                                availabilityEstimation += instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
                        }
                        availabilityEstimation /= instances.size() - 1;
                    }
                }
                if (ChangeImplementationOption.class.equals(adaptationOption.getClass())) {
                    ChangeImplementationOption changeImplementationOption = (ChangeImplementationOption) adaptationOption;
                    availabilityEstimation = service.getPossibleImplementations().get(changeImplementationOption.getNewImplementationId()).getBenchmark(Availability.class);
                }
                double newBenefit = availabilityEstimation / service.getCurrentValueForQoS(Availability.class).getDoubleValue();
                log.debug(service.getServiceId() + ": " + adaptationOption.getClass().getSimpleName() + " option for Availability. BENEFIT: " + newBenefit);

                if (newBenefit > 1 && (!benefits.containsKey(Availability.class) || newBenefit > benefits.get(Availability.class))) {
                    benefits.put(Availability.class, newBenefit);
                    bestOptionForGoal.put(Availability.class, adaptationOption);
                }
            }
            else if(adaptationOption.getQosGoal() == AverageResponseTime.class){
                double avgResponseTimeEstimation = 0.0;
                if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
                    if (ChangeLoadBalancerWeightsOption.class.equals(adaptationOption.getClass())) {
                        ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) adaptationOption;
                        for (Instance instance : instances) {
                            if(!changeLoadBalancerWeightsOption.getInstancesToShutdownIds().contains(instance.getInstanceId()))
                                avgResponseTimeEstimation += changeLoadBalancerWeightsOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
                        }
                    } else if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        AddInstanceOption addInstanceOption = (AddInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if(!addInstanceOption.getInstancesToShutdownIds().contains(instance.getInstanceId()))
                                avgResponseTimeEstimation += addInstanceOption.getOldInstancesNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
                        }
                        avgResponseTimeEstimation += addInstanceOption.getNewInstanceWeight() * service.getCurrentImplementation().getBenchmark(AverageResponseTime.class);
                    } else if (ShutdownInstanceOption.class.equals(adaptationOption.getClass())) {
                        ShutdownInstanceOption shutdownInstanceOption = (ShutdownInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(shutdownInstanceOption.getInstanceToShutdownId()))
                                avgResponseTimeEstimation += shutdownInstanceOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
                        }
                    }

                }
                else {
                    if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        for (Instance instance : instances) {
                            avgResponseTimeEstimation += instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
                        }
                        avgResponseTimeEstimation += service.getCurrentImplementation().getBenchmark(AverageResponseTime.class);
                        avgResponseTimeEstimation /= instances.size() + 1;
                    } else if (ShutdownInstanceOption.class.equals(adaptationOption.getClass())) {
                        ShutdownInstanceOption shutdownInstanceOption = (ShutdownInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(shutdownInstanceOption.getInstanceToShutdownId()))
                                avgResponseTimeEstimation += instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
                        }
                        avgResponseTimeEstimation /= instances.size() - 1;
                    }

                }
                if(ChangeImplementationOption.class.equals(adaptationOption.getClass())) {
                    ChangeImplementationOption changeImplementationOption = (ChangeImplementationOption) adaptationOption;
                    avgResponseTimeEstimation = service.getPossibleImplementations().get(changeImplementationOption.getNewImplementationId()).getBenchmark(AverageResponseTime.class);
                }
                double newBenefit =  service.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue() / avgResponseTimeEstimation;
                log.debug(service.getServiceId() + ": " + adaptationOption.getClass().getSimpleName() + " option for AVG RT. BENEFIT: " + newBenefit);
                if(newBenefit > 1 && (!benefits.containsKey(AverageResponseTime.class) || newBenefit > benefits.get(AverageResponseTime.class))){
                    benefits.put(AverageResponseTime.class, newBenefit);
                    bestOptionForGoal.put(AverageResponseTime.class, adaptationOption);
                }
            }
        }

        Class<? extends QoSSpecification> bestBenefitClass = null;

        for (Class<? extends QoSSpecification> qosSpecification : benefits.keySet()) {
            double newPreference = service.getQoSSpecifications().get(qosSpecification).getWeight();
            if(bestBenefitClass == null || benefits.get(qosSpecification) * newPreference > benefits.get(bestBenefitClass) * service.getQoSSpecifications().get(bestBenefitClass).getWeight())
                bestBenefitClass = qosSpecification;
        }
        if (bestBenefitClass == null) {
            log.warn("{}: No beneficial adaptation option", service.getServiceId());
            return null;
        }


        log.debug("\n\t\t\t\t{}: Selected option {} for {} with benefit {}. \n\t\t\t\tDetails: {}", service.getServiceId(), bestOptionForGoal.get(bestBenefitClass).getClass().getSimpleName(), bestBenefitClass.getSimpleName(), benefits.get(bestBenefitClass), bestOptionForGoal.get(bestBenefitClass));
        return bestOptionForGoal.get(bestBenefitClass);
    }

    /**
     * Redistributes the weight of an instance that will be shutdown to all the other instances of the service.
     *
     * @param originalWeights the original weights of the instances
     * @param instancesToRemoveIds the instances that will be shutdown
     * @return the new originalWeights map. It does not contain the instances that will be shutdown
     */
    private Map<String, Double> redistributeWeight(Map<String, Double> originalWeights, List<String> instancesToRemoveIds) {
        Map<String, Double> newWeights = new HashMap<>();
        double totalWeightToRemove = originalWeights.entrySet().stream().filter(entry -> instancesToRemoveIds.contains(entry.getKey())).mapToDouble(Map.Entry::getValue).sum();
        int newSizeOfActiveInstances = originalWeights.size() - instancesToRemoveIds.size();
        double weightToAdd = totalWeightToRemove / newSizeOfActiveInstances;
        for (String instanceId : originalWeights.keySet()) {
            if (!instancesToRemoveIds.contains(instanceId)) { // if the instance is not in the list of instances to shutdown
                double weight = originalWeights.get(instanceId) + weightToAdd; // increment the weight of the instance with 1/newSizeOfActiveInstances of the total weight to remove
                newWeights.put(instanceId, weight);
            }
        }
        return newWeights;
    }

    /**
     * Reduces the active instances weight to give enough weight to the new instance.
     * The weights map parameter is not modified.
     *
     * @param weights the original weights of the active instances. This is not affected by the changes
     * @return the new weights map
     */
    private Map<String, Double> reduceWeightsForNewInstance(Map<String, Double> weights, int newNumberOfNewInstances) {
        Map<String, Double> newWeights = new HashMap<>();
        int oldNumberOfInstances = weights.size();
        for (String instanceId : weights.keySet()) {
            newWeights.put(instanceId, weights.get(instanceId) * (double) oldNumberOfInstances / (double) (oldNumberOfInstances+newNumberOfNewInstances));
        }
        return newWeights;
    }

}

