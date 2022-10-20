package it.polimi.saefa.plan.domain;

import com.google.ortools.linearsolver.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.architecture.*;
import it.polimi.saefa.plan.externalInterfaces.ExecuteClient;
import it.polimi.saefa.plan.externalInterfaces.KnowledgeClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.util.*;

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

    static {
        try {
            System.load(ResourceUtils.getFile("classpath:libjniortools.dylib").getAbsolutePath());
            System.load(ResourceUtils.getFile("classpath:libortools.9.dylib").getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Error loading or-tools libraries", e);
        }
    }

    //For a given service, the system must not be in a transition state (i.e. booting instances).
    // In that case, only forced adaptation options are allowed.
    public void startPlan() {
        try {
            log.info("Starting plan");
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
                            log.debug(option.getDescription());
                            if (option.getClass().equals(ChangeLoadBalancerWeightsOption.class)) {
                                ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) option;
                                Map<String, Double> newWeights = handleChangeLoadBalancerWeights(servicesMap.get(option.getServiceId()));
                                if (newWeights != null) { //If it's null it means that the problem has no solution
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
                                handleRemoveInstance((ShutdownInstanceOption) option, servicesMap.get(option.getServiceId()), false);
                            if (option.getClass().equals(ChangeImplementationOption.class))
                                handleChangeImplementation((ChangeImplementationOption) option, servicesMap.get(option.getServiceId()));
                        }
                        AdaptationOption chosenOption = extractBestOption(servicesMap.get(serviceId), proposedAdaptationOptions.get(serviceId));
                        if (chosenOption != null)
                            chosenAdaptationOptionList.add(chosenOption);
                    } else {
                        // If there is at least a forced option, all the other options are ignored
                        log.debug("{} has forced Adaptation options", serviceId);

                        //We first perform all the ShutdownInstanceOptions and then perform the final AddInstanceOption, if any. This same order must be respected by the Execute.
                        for (AdaptationOption option : forcedAdaptationOptions.stream().filter(option -> option.getClass().equals(ShutdownInstanceOption.class)).toList()) {
                            log.debug(option.toString());
                            chosenAdaptationOptionList.add(handleRemoveInstance((ShutdownInstanceOption) option, servicesMap.get(option.getServiceId()), true));
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
                    chosenAdaptationOptions.put(serviceId, chosenAdaptationOptionList);
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

    //quando è forced vanno aggiornati i pesi nel servizio, in modo che tutte le altre opzioni di adattamento siano coerenti con le opzioni che verranno applicate
    private ShutdownInstanceOption handleRemoveInstance(ShutdownInstanceOption shutdownInstanceOption, Service service, boolean isForced) {
        if(service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM){
            shutdownInstanceOption.setNewWeights(redistributeWeight(service.getLoadBalancerWeights(), shutdownInstanceOption.getInstanceToShutdownId()));
            if(isForced){
                service.setLoadBalancerWeights(shutdownInstanceOption.getNewWeights());
                service.removeInstance(service.getInstancesMap().get(shutdownInstanceOption.getInstanceToShutdownId()));
            }
        }
        return shutdownInstanceOption;
    }

    //We assume that only one AddInstance option per service for each loop iteration is proposed by the Analyse module.
    private AddInstanceOption handleAddInstance(AddInstanceOption addInstanceOption, Service service) {
        if(service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
            Set<String> instancesToRemove = new HashSet<>();
            double shutdownThreshold = service.getCurrentImplementation().getInstanceLoadShutdownThreshold() / (service.getInstances().size()+1);
            boolean allWeightsOverThreshold = false;
            Map<String, Double> oldWeightsRedistributed = service.getLoadBalancerWeights();

            // TODO valuta ricorsione
            while(!allWeightsOverThreshold) {
                List<String> instancesToRemoveCurrentIteration = new LinkedList<>();
                allWeightsOverThreshold = true;
                int newNumberOfInstances = oldWeightsRedistributed.size()+1;
                oldWeightsRedistributed = reduceWeightsForNewInstance(oldWeightsRedistributed, newNumberOfInstances);
                for (String instanceId : oldWeightsRedistributed.keySet()) {
                    if (oldWeightsRedistributed.get(instanceId) < shutdownThreshold) {
                        instancesToRemoveCurrentIteration.add(instanceId);
                        allWeightsOverThreshold = false;
                        // TODO verifica che sia corretta
                        log.debug("Instance {} has weight {} and will be removed. Shutdown threshold: {}", instanceId, oldWeightsRedistributed.get(instanceId), shutdownThreshold);
                        shutdownThreshold = shutdownThreshold*(newNumberOfInstances-instancesToRemoveCurrentIteration.size()-1)/(newNumberOfInstances-instancesToRemoveCurrentIteration.size());
                        log.debug("New shutdown threshold: {}", shutdownThreshold);
                    }
                }
                for (String instanceId : instancesToRemoveCurrentIteration) {
                    oldWeightsRedistributed = redistributeWeight(oldWeightsRedistributed, instanceId);
                }
                instancesToRemove.addAll(instancesToRemoveCurrentIteration);
            }

            int newNumberOfInstances = oldWeightsRedistributed.size()+1;
            Double newWeight = 1.0 / (newNumberOfInstances);

            addInstanceOption.setNewInstanceWeight(newWeight);
            addInstanceOption.setOldInstancesNewWeights(oldWeightsRedistributed);
            addInstanceOption.setInstancesToShutdownIds(instancesToRemove.stream().toList());
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

            // w_i - a_i*shutdownThreshold >= 0 OVVERO
            // w_i >= a_i * shutdownThreshold
            MPConstraint lowerBoundConstraint = solver.makeConstraint(0, Double.POSITIVE_INFINITY, instance.getInstanceId() + "_activation_lowerBoundConstraint");
            lowerBoundConstraint.setCoefficient(weight, 1);
            lowerBoundConstraint.setCoefficient(activation, -shutdownThreshold);

            // w_i - a_i<=0 OVVERO
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

            if (k_i == 0) { //fatto nel for di sopra
                //MPConstraint forceZeroWeight = solver.makeConstraint(0, 0, instance_i.getInstanceId() + "_forceZeroWeight");
                //forceZeroWeight.setCoefficient(weight_i, 1);
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
                    // w_i - k_i/k_j * w_j + a_j  <= 1 OVVERO
                    // w_i <= k_i/k_j * w_j + (1 - a_j)
                    MPConstraint weightsBalancingConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1, instance_i + "constraint4");
                    weightsBalancingConstraint.setCoefficient(weight_i, 1);
                    weightsBalancingConstraint.setCoefficient(weight_j, -k_ij);
                    weightsBalancingConstraint.setCoefficient(activationsVariables.get(instance_j.getInstanceId()), 1);
                }
                    /*

                    else{// Caso k_i<k_j il costraint viene implicato da sopra
                        // w_i - k_i/k_j * w_j - a_i  >= -1 OVVERO
                        // w_i >= k_i/k_j * w_j - (1 - a_i)
                        MPConstraint maxDecreaseUpperBoundConstraint = solver.makeConstraint(-1, Double.POSITIVE_INFINITY, instance_i + "maxDecreaseUpperBoundConstraint");
                        maxDecreaseUpperBoundConstraint.setCoefficient(weight_i, 1);
                        maxDecreaseUpperBoundConstraint.setCoefficient(weight_j, -z_ij);
                        maxDecreaseUpperBoundConstraint.setCoefficient(activations.get(instance_i.getInstanceId()), -1);
                    }
                   */


            }
        }

        objective.setMinimization();
        final MPSolver.ResultStatus resultStatus = solver.solve();

        StringBuilder sb = new StringBuilder();
        sb.append("\n Minimization problem for service ").append(service.getServiceId()).append(" solved with status ").append(resultStatus);
        sb.append("\nSoglia: ").append(shutdownThreshold).append("\n");
        sb.append("Service response time: ").append(serviceAvgRespTime).append("\n");
        sb.append("Service availability: ").append(serviceAvgAvailability).append("\n");
        sb.append("Service k_s: ").append(k_s).append("\n");
        sb.append("\nSolution: \n");
        sb.append("Objective value = ").append(objective.value()).append("\n");

        for (String instanceId : weightsVariables.keySet()) {
            String P_i = String.format("%.2f", previousWeights.get(instanceId));
            double avail_i_double = service.getInstance(instanceId).getCurrentValueForQoS(Availability.class).getDoubleValue();
            String avail_i = String.format("%.2f", avail_i_double);
            double ART_i_double = service.getInstance(instanceId).getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
            String ART_i = String.format("%.2f", ART_i_double);
            double k_i_double = avail_i_double/ART_i_double;
            String k_i = String.format("%.2f", k_i_double);
            double z_i_double = k_i_double/k_s;
            String z_i = String.format("%.2f", z_i_double);
            sb.append(instanceId + " { P_i="+P_i+", k_i="+k_i+", z_i="+z_i+", ART_i="+ART_i+", avail_i="+avail_i+" }\n");
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
        //Deve prendere la lista di possible implementation,
        for(String implementationId: changeImplementationOption.getPossibleImplementations()){
            Class<? extends QoSSpecification> goal = changeImplementationOption.getQosGoal();
            ServiceImplementation implementation = service.getPossibleImplementations().get(implementationId);
            double benchmark = implementation.getBenchmark(changeImplementationOption.getQosGoal()) * implementation.getScore();
            if(bestImplementationId == null) {
                bestImplementationId = implementationId;
                bestImplementationBenefit = benchmark;
            }
            else{
                if(Availability.class == goal) {
                    if (benchmark > bestImplementationBenefit) {
                        bestImplementationId = implementationId;
                        bestImplementationBenefit = benchmark;
                    }
                } else if(AverageResponseTime.class == goal) {
                    if (benchmark < bestImplementationBenefit) {
                        bestImplementationId = implementationId;
                        bestImplementationBenefit = benchmark;
                    }
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
            if(adaptationOption.getQosGoal() == Availability.class) {
                double availabilityEstimation = 0.0;
                if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
                    if (ChangeLoadBalancerWeightsOption.class.equals(adaptationOption.getClass())) {
                        ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) adaptationOption;
                        for (Instance instance : instances) {
                            if(!changeLoadBalancerWeightsOption.getInstancesToShutdownIds().contains(instance.getInstanceId()))
                                availabilityEstimation += changeLoadBalancerWeightsOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
                        }
                    }
                    else if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        AddInstanceOption addInstanceOption = (AddInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if(!addInstanceOption.getInstancesToShutdownIds().contains(instance.getInstanceId()))
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
                if(ChangeImplementationOption.class.equals(adaptationOption.getClass())) {
                    ChangeImplementationOption changeImplementationOption = (ChangeImplementationOption) adaptationOption;
                    availabilityEstimation = service.getPossibleImplementations().get(changeImplementationOption.getNewImplementationId()).getBenchmark(Availability.class);
                }
                double newBenefit = availabilityEstimation / service.getCurrentValueForQoS(Availability.class).getDoubleValue();
                log.debug(service.getServiceId() + ": " + adaptationOption.getClass().getSimpleName() + " option for Availability. BENEFIT: " + newBenefit);

                if(newBenefit > 1 && (!benefits.containsKey(Availability.class) || newBenefit > benefits.get(Availability.class))){
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
            if(bestBenefitClass == null || benefits.get(qosSpecification) > benefits.get(bestBenefitClass))
                bestBenefitClass = qosSpecification;
        }
        if (bestBenefitClass == null) {
            log.warn("{}: No beneficial adaptation option", service.getServiceId());
            return null;
        }

        log.debug("\n\t\t\t\t{}: Selected option {} for {} with benefit {}. \n\t\t\t\tDetails: {}", service.getServiceId(), bestOptionForGoal.get(bestBenefitClass).getClass().getSimpleName(), bestBenefitClass.getSimpleName(), benefits.get(bestBenefitClass), bestOptionForGoal.get(bestBenefitClass));
        return bestOptionForGoal.get(bestBenefitClass);
    }

    public void breakpoint(){
        log.info("breakpoint");
    }

    /**
     * Redistributes the weight of an instance that will be shutdown to all the other instances of the service.
     *
     * @param originalWeights
     * @param instanceToRemoveId
     * @return the new originalWeights map
     */
    private Map<String, Double> redistributeWeight(Map<String, Double> originalWeights, String instanceToRemoveId) {
        Map<String, Double> newWeights = new HashMap<>();
        double instanceWeight = originalWeights.get(instanceToRemoveId);
        for (String instanceId : originalWeights.keySet()) {
            double weight = originalWeights.get(instanceId);
            if (!instanceId.equals(instanceToRemoveId)) {
                weight += instanceWeight / (originalWeights.size() - 1);
                newWeights.put(instanceId, weight);
            }
        }
        return newWeights;
    }

    /**
     * Reduces the active instances weight to give enough weight to the new instance.
     *
     * @param weights
     * @return the new weights map
     */
    private Map<String, Double> reduceWeightsForNewInstance(Map<String, Double> weights, int newNumberOfInstances) {
        Map<String, Double> newWeights = new HashMap<>();
        int oldNumberOfInstances = weights.size();

        for (String instanceId : weights.keySet()) {
            newWeights.put(instanceId, weights.get(instanceId) * (double) oldNumberOfInstances / (double) newNumberOfInstances);
        }
        return newWeights;
    }

}

