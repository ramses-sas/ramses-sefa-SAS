package it.polimi.saefa.plan.domain;

import com.google.ortools.linearsolver.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.architecture.*;
import it.polimi.saefa.plan.externalInterfaces.ExecuteClient;
import it.polimi.saefa.plan.externalInterfaces.KnowledgeClient;
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
            Map<String, List<AdaptationOption>> proposedAdaptationOptions = knowledgeClient.getProposedAdaptationOptions();
            Map<String, Service> servicesMap = knowledgeClient.getServicesMap();
            Map<String, List<AdaptationOption>> chosenAdaptationOptions = new HashMap<>();

            proposedAdaptationOptions.forEach((serviceId, options) -> {
                log.debug("\n\nAnalysing service: {}", serviceId);
                List<AdaptationOption> chosenAdaptationOptionList = new LinkedList<>();
                // Initialized with all the forced options
                List<AdaptationOption> forcedAdaptationOptions = new LinkedList<>(options.stream().filter(AdaptationOption::isForced).toList());
                List<AdaptationOption> optionsToCompare = new LinkedList<>();

                if (forcedAdaptationOptions.isEmpty()) {
                    for (AdaptationOption option : options) {
                        log.info("Adaptation option: {}", option.getDescription());
                        if (option.getClass().equals(ChangeLoadBalancerWeightsOption.class)) {
                            ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) option;
                            changeLoadBalancerWeightsOption.setNewWeights(handleChangeLoadBalancerWeights(servicesMap.get(option.getServiceId())));
                            if (changeLoadBalancerWeightsOption.getNewWeights() != null) //If it's null it means that the problem has no solution
                                optionsToCompare.add(changeLoadBalancerWeightsOption);
                        }
                        if (option.getClass().equals(AddInstanceOption.class))
                            optionsToCompare.add(handleAddInstance((AddInstanceOption) option, servicesMap.get(option.getServiceId())));
                        if (option.getClass().equals(RemoveInstanceOption.class))
                            optionsToCompare.add(handleRemoveInstance((RemoveInstanceOption) option, servicesMap.get(option.getServiceId()), false));
                        if (option.getClass().equals(ChangeImplementationOption.class))
                            optionsToCompare.add(handleChangeImplementation((ChangeImplementationOption) option, servicesMap.get(option.getServiceId())));
                    }
                    AdaptationOption chosenOption = extractBestOption(servicesMap.get(serviceId), optionsToCompare);
                    if (chosenOption != null)
                        chosenAdaptationOptionList.add(chosenOption);
                }
                else {
                    // If there is at least a forced option, all the other options are ignored
                    log.info("Forced adaptation options: {}", forcedAdaptationOptions);

                    //We first perform all the RemoveOptions and then perform the final AddInstanceOption, if any. This same order must be respected by the Execute.
                    for(AdaptationOption option : forcedAdaptationOptions.stream().filter(option -> option.getClass().equals(RemoveInstanceOption.class)).toList()) {
                        chosenAdaptationOptionList.add(handleRemoveInstance((RemoveInstanceOption) option, servicesMap.get(option.getServiceId()), true));
                    }
                    List<AddInstanceOption> addInstanceOptions = forcedAdaptationOptions.stream().filter(option -> option.getClass().equals(AddInstanceOption.class)).map(option -> (AddInstanceOption) option).toList();
                    if (addInstanceOptions.size()>1) {
                        log.error("More than one add instance option is forced");
                        throw new RuntimeException("More than one add instance option is forced");
                    }else if (addInstanceOptions.size()==1) {
                        chosenAdaptationOptionList.add(handleAddInstance(addInstanceOptions.get(0), servicesMap.get(addInstanceOptions.get(0).getServiceId())));
                    }
                }
                chosenAdaptationOptions.put(serviceId, chosenAdaptationOptionList);
            });
            knowledgeClient.chooseAdaptationOptions(chosenAdaptationOptions);
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
    private RemoveInstanceOption handleRemoveInstance(RemoveInstanceOption removeInstanceOption, Service service, boolean isForced) {
        if(service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM){
            removeInstanceOption.setNewWeights(redistributeWeight(service.getLoadBalancerWeights(), removeInstanceOption.getInstanceId()));
            if(isForced){
                service.setLoadBalancerWeights(removeInstanceOption.getNewWeights());
                service.removeInstance(service.getInstancesMap().get(removeInstanceOption.getInstanceId()));
            }
        }
        return removeInstanceOption;
    }

    //We assume that only one AddInstance option per service for each loop iteration is proposed by the Analyse module.
    private AddInstanceOption handleAddInstance(AddInstanceOption addInstanceOption, Service service) {
        if(service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
            int oldNumberOfInstances = service.getInstances().size();
            int newNumberOfInstances = oldNumberOfInstances + 1;
            Map<String, Double> oldWeightsRedistributed = reduceWeightsForNewInstance(service.getLoadBalancerWeights(), newNumberOfInstances);

            Set<String> instancesToRemove = new HashSet<>();
            double shutdownThreshold = service.getCurrentImplementation().getInstanceLoadShutdownThreshold() / service.getInstances().size();

            Double newWeight = 1.0 / (newNumberOfInstances);
            for (String instanceId : oldWeightsRedistributed.keySet()) {
                if (oldWeightsRedistributed.get(instanceId) < shutdownThreshold) {
                    instancesToRemove.add(instanceId);
                }
            }
            for (String instanceId : instancesToRemove) {
                oldWeightsRedistributed = redistributeWeight(oldWeightsRedistributed, instanceId);
            }
            addInstanceOption.setNewInstanceWeight(newWeight);
            addInstanceOption.setOldInstancesNewWeights(oldWeightsRedistributed);
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
        Map<String, MPVariable> weights = new HashMap<>();
        Map<String, MPVariable> activations = new HashMap<>();
        MPObjective objective = solver.objective();// min{∑(w_i/z_i) - ∑(a_i * z_i)}

        double serviceAvgRespTime = service.getCurrentValueForParam(AverageResponseTime.class).getValue();
        double serviceAvgAvailability = service.getCurrentValueForParam(Availability.class).getValue();
        double k_s = serviceAvgAvailability / serviceAvgRespTime; // service performance indicator
        MPConstraint sumOfWeights = solver.makeConstraint("sumOfWeights"); //∑w_i = 1

        for (Instance instance : service.getInstances()) {
                MPVariable weight = solver.makeNumVar(0, 1, instance.getInstanceId() + "_weight");
                MPVariable activation = solver.makeIntVar(0, 1, instance.getInstanceId() + "_activation");
                weights.put(instance.getInstanceId(), weight);
                activations.put(instance.getInstanceId(), activation);
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

                double instanceAvgRespTime = instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
                double instanceAvailability = instance.getCurrentValueForParam(Availability.class).getValue();
                double k_i = instanceAvailability / instanceAvgRespTime;
                double z_i = k_i / k_s;

                if (k_i != 0.0) {
                    objective.setCoefficient(weight, 1 / z_i);
                    objective.setCoefficient(activation, -z_i);
                }
        }

        for (Instance instance_i : service.getInstances()) {

            MPVariable weight_i = weights.get(instance_i.getInstanceId());
            double instanceAvgRespTime_i = instance_i.getCurrentValueForParam(AverageResponseTime.class).getValue();
            double instanceAvailability_i = instance_i.getCurrentValueForParam(Availability.class).getValue();
            double k_i = instanceAvailability_i / instanceAvgRespTime_i;
            double z_i = k_i / k_s;

            if (k_i == 0) {
                MPConstraint forceZeroWeight = solver.makeConstraint(0, 0, instance_i.getInstanceId() + "_forceZeroWeight");
                forceZeroWeight.setCoefficient(weight_i, 1);
                continue;
            }

            MPConstraint firstConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, z_i, instance_i + "firstConstraint"); // w_i<= p_i * k_i/k_s + soglia * (n - ∑a_i)
            firstConstraint.setCoefficient(weight_i, 1);

            for (Instance instance_j : service.getInstances()) {
                MPVariable weight_j = weights.get(instance_j.getInstanceId());
                double instanceAvgRespTime_j = instance_j.getCurrentValueForParam(AverageResponseTime.class).getValue();
                double instanceAvailability_j = instance_j.getCurrentValueForParam(Availability.class).getValue();
                firstConstraint.setCoefficient(activations.get(instance_j.getInstanceId()), z_i * previousWeights.get(instance_j.getInstanceId()));

                double k_j = instanceAvailability_j / instanceAvgRespTime_j;
                double z_ij = k_i / k_j;

                if (k_i >= k_j) {
                    // w_i - k_i/k_j * w_j + a_j  <= 1 OVVERO
                    // w_i <= k_i/k_j * w_j + (1 - a_j)
                    MPConstraint maxGrowthUpperBoundConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1, instance_i + "maxGrowthUpperBoundConstraint");
                    maxGrowthUpperBoundConstraint.setCoefficient(weight_i, 1);
                    maxGrowthUpperBoundConstraint.setCoefficient(weight_j, -z_ij);
                    maxGrowthUpperBoundConstraint.setCoefficient(activations.get(instance_j.getInstanceId()), 1);
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

        StringBuffer sb = new StringBuffer();
        sb.append("\nSoglia: ").append(shutdownThreshold).append("\n");
        sb.append("Service response time: ").append(serviceAvgRespTime).append("\n");
        sb.append("Service availability: ").append(serviceAvgAvailability).append("\n");
        sb.append("Service k_s: ").append(k_s).append("\n");
        sb.append("\nSolution: \n");
        sb.append("Objective value = ").append(objective.value()).append("\n");

        for (String instanceId : weights.keySet()) {
            String P_i = String.format("%.2f", previousWeights.get(instanceId));
            double avail_i_double = service.getInstance(instanceId).getCurrentValueForParam(Availability.class).getValue();
            String avail_i = String.format("%.2f", avail_i_double);
            double ART_i_double = service.getInstance(instanceId).getCurrentValueForParam(AverageResponseTime.class).getValue();
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

        if(previousWeights.size() != weights.size()){
            throw new RuntimeException("The number of weights is not the same as the number of instances!");
        }

        for (String instanceId : weights.keySet()) {
            if(weights.get(instanceId) == null)
                throw new RuntimeException("Instance " + instanceId + " not found in the weights map");
            double W_i_double = weights.get(instanceId).solutionValue();
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
            Class<? extends AdaptationParamSpecification> goal = changeImplementationOption.getAdaptationParametersGoal();
            ServiceImplementation implementation = service.getPossibleImplementations().get(implementationId);
            double benchmark = implementation.getBootBenchmark(changeImplementationOption.getAdaptationParametersGoal()) * implementation.getScore();
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
        Map<Class<? extends AdaptationParamSpecification>, Double> benefits = new HashMap<>();
        Map<Class<? extends AdaptationParamSpecification>, AdaptationOption> bestOptionForGoal = new HashMap<>();

        for (AdaptationOption adaptationOption : toCompare) {
            List<Instance> instances = service.getInstances();
            if(adaptationOption.getAdaptationParametersGoal() == Availability.class) {
                double availabilityEstimation = 0.0;
                if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
                    if (ChangeLoadBalancerWeightsOption.class.equals(adaptationOption.getClass())) {
                        ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) adaptationOption;
                        for (Instance instance : instances) {
                            availabilityEstimation += changeLoadBalancerWeightsOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForParam(Availability.class).getValue();
                        }
                    }
                    else if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        AddInstanceOption addInstanceOption = (AddInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            availabilityEstimation += addInstanceOption.getOldInstancesNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForParam(Availability.class).getValue();
                        }
                        availabilityEstimation += addInstanceOption.getNewInstanceWeight() * service.getCurrentImplementation().getBootBenchmark(Availability.class);
                    } else if (RemoveInstanceOption.class.equals(adaptationOption.getClass())) {
                        RemoveInstanceOption removeInstanceOption = (RemoveInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(removeInstanceOption.getInstanceId()))
                                availabilityEstimation += removeInstanceOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForParam(Availability.class).getValue();
                        }
                    }

                }
                else {
                    if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        for (Instance instance : instances) {
                            availabilityEstimation += instance.getCurrentValueForParam(Availability.class).getValue();
                        }
                        availabilityEstimation += service.getCurrentImplementation().getBootBenchmark(Availability.class);
                        availabilityEstimation /= instances.size() + 1;
                    } else if (RemoveInstanceOption.class.equals(adaptationOption.getClass())) {
                        RemoveInstanceOption removeInstanceOption = (RemoveInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(removeInstanceOption.getInstanceId()))
                                availabilityEstimation += instance.getCurrentValueForParam(Availability.class).getValue();
                        }
                        availabilityEstimation /= instances.size() - 1;
                    }
                }
                if(ChangeImplementationOption.class.equals(adaptationOption.getClass())) {
                    ChangeImplementationOption changeImplementationOption = (ChangeImplementationOption) adaptationOption;
                    availabilityEstimation = service.getPossibleImplementations().get(changeImplementationOption.getNewImplementationId()).getBootBenchmark(Availability.class);
                }
                double newBenefit = availabilityEstimation / service.getCurrentValueForParam(Availability.class).getValue();
                log.debug("New benefit brought by " + adaptationOption.getClass().getSimpleName() + " for availability: " + newBenefit);

                if(newBenefit > 1 && (!benefits.containsKey(Availability.class) || newBenefit > benefits.get(Availability.class))){
                    benefits.put(Availability.class, newBenefit);
                    bestOptionForGoal.put(Availability.class, adaptationOption);
                }
            }
            else if(adaptationOption.getAdaptationParametersGoal() == AverageResponseTime.class){
                double avgResponseTimeEstimation = 0.0;
                if (service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
                    if (ChangeLoadBalancerWeightsOption.class.equals(adaptationOption.getClass())) {
                        ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption = (ChangeLoadBalancerWeightsOption) adaptationOption;
                        for (Instance instance : instances) {
                            avgResponseTimeEstimation += changeLoadBalancerWeightsOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
                        }
                    } else if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        AddInstanceOption addInstanceOption = (AddInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            avgResponseTimeEstimation += addInstanceOption.getOldInstancesNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
                        }
                        avgResponseTimeEstimation += addInstanceOption.getNewInstanceWeight() * service.getCurrentImplementation().getBootBenchmark(AverageResponseTime.class);
                    } else if (RemoveInstanceOption.class.equals(adaptationOption.getClass())) {
                        RemoveInstanceOption removeInstanceOption = (RemoveInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(removeInstanceOption.getInstanceId()))
                                avgResponseTimeEstimation += removeInstanceOption.getNewWeights().get(instance.getInstanceId()) * instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
                        }
                    }

                }
                else {
                    if (AddInstanceOption.class.equals(adaptationOption.getClass())) {
                        for (Instance instance : instances) {
                            avgResponseTimeEstimation += instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
                        }
                        avgResponseTimeEstimation += service.getCurrentImplementation().getBootBenchmark(AverageResponseTime.class);
                        avgResponseTimeEstimation /= instances.size() + 1;
                    } else if (RemoveInstanceOption.class.equals(adaptationOption.getClass())) {
                        RemoveInstanceOption removeInstanceOption = (RemoveInstanceOption) adaptationOption;
                        for (Instance instance : instances) {
                            if (!instance.getInstanceId().equals(removeInstanceOption.getInstanceId()))
                                avgResponseTimeEstimation += instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
                        }
                        avgResponseTimeEstimation /= instances.size() - 1;
                    }

                }
                if(ChangeImplementationOption.class.equals(adaptationOption.getClass())) {
                    ChangeImplementationOption changeImplementationOption = (ChangeImplementationOption) adaptationOption;
                    avgResponseTimeEstimation = service.getPossibleImplementations().get(changeImplementationOption.getNewImplementationId()).getBootBenchmark(AverageResponseTime.class);
                }
                double newBenefit =  service.getCurrentValueForParam(AverageResponseTime.class).getValue() / avgResponseTimeEstimation;
                log.debug("New benefit brought by " + adaptationOption.getClass().getSimpleName() + " for response time: " + newBenefit);
                if(newBenefit > 1 && (!benefits.containsKey(AverageResponseTime.class) || newBenefit > benefits.get(AverageResponseTime.class))){
                    benefits.put(AverageResponseTime.class, newBenefit);
                    bestOptionForGoal.put(AverageResponseTime.class, adaptationOption);
                }
            }
        }

        Class<? extends AdaptationParamSpecification> bestBenefitClass = null;

        for (Class<? extends AdaptationParamSpecification> adaptationParamSpecification : benefits.keySet()) {
            if(bestBenefitClass == null || benefits.get(adaptationParamSpecification) > benefits.get(bestBenefitClass))
                bestBenefitClass = adaptationParamSpecification;
        }
        if(bestBenefitClass == null){
            log.error("No beneficial adaptation option found for service " + service.getServiceId());
            return null;
        }

        return bestOptionForGoal.get(bestBenefitClass);
    }

    public void breakpoint(){
        log.info("breakpoint");
    }

    /**
     * Redistributes the weight of an instance that will be shutdown to all the other instances of the service.
     *
     * @param weights
     * @param instanceToRemoveId
     * @return the new weights map
     */
    private Map<String, Double> redistributeWeight(Map<String, Double> weights, String instanceToRemoveId) {
        Map<String, Double> newWeights = new HashMap<>();
        double instanceWeight = weights.get(instanceToRemoveId);
        for(String instanceId : weights.keySet()) {
            double weight = weights.get(instanceId);
            if (!instanceId.equals(instanceToRemoveId)) {
                weight += instanceWeight / (weights.size() - 1);
                newWeights.put(instanceId, weight);
            } else
                newWeights.put(instanceId, 0.0);
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


    public Map<String, Double> handleChangeLoadBalancerWeightsTEST() {
        log.warn("TEST");
        double soglia = 0.3;
        soglia = Math.random();
        Map<String, Double> previousWeights = new HashMap<>();
        previousWeights.put("1", Math.random());
        previousWeights.put("2", Math.random() * (1 - previousWeights.get("1")));
        previousWeights.put("3", Math.random() * (1 - previousWeights.get("1") - previousWeights.get("2")));
        previousWeights.put("4", 1-previousWeights.get("1")-previousWeights.get("2")-previousWeights.get("3"));

        /*
        previousWeights.put("1", 0.643);
        previousWeights.put("2", 0.102);
        previousWeights.put("3", 0.066);
        previousWeights.put("4", 1-previousWeights.get("1")-previousWeights.get("2")-previousWeights.get("3"));

         */

        Map<String, Double> instanceAvailabilities = new HashMap<>();
        instanceAvailabilities.put("1", Math.random());
        instanceAvailabilities.put("2", Math.random());
        instanceAvailabilities.put("3", Math.random());
        instanceAvailabilities.put("4", Math.random());

        /*
        instanceAvailabilities.put("1", 0.197);
        instanceAvailabilities.put("2", 0.964);
        instanceAvailabilities.put("3", 0.521);
        instanceAvailabilities.put("4", 0.101);

         */

        Map<String, Double> instanceResponseTimes = new HashMap<>();
        instanceResponseTimes.put("1", Math.random()*10);
        instanceResponseTimes.put("2", Math.random()*10);
        instanceResponseTimes.put("3", Math.random()*10);
        instanceResponseTimes.put("4", Math.random()*10);

        /*
        instanceResponseTimes.put("1", 9.364);
        instanceResponseTimes.put("2", 8.876);
        instanceResponseTimes.put("3", 1.258);
        instanceResponseTimes.put("4", 1.413);

         */


        double serviceAvgRespTime = instanceResponseTimes.values().stream().reduce(0.0, Double::sum)/instanceResponseTimes.size();
        double serviceAvailability = instanceAvailabilities.values().stream().reduce(0.0, Double::sum)/instanceAvailabilities.size();
        double k_s = serviceAvailability/serviceAvgRespTime; //performance indicator


        Map<String, Double> performanceRatio = new HashMap<>();
        Map<String, Double> newWeights = new HashMap<>();

        MPSolver solver = MPSolver.createSolver("SCIP");
        MPObjective objective = solver.objective();
        Map<String, MPVariable> weights = new HashMap<>();
        Map<String, MPVariable> activations = new HashMap<>();

        MPVariable sumOfActivationsValue = solver.makeNumVar(0, Double.POSITIVE_INFINITY , "sumOfActivationsValue");
        MPConstraint sumOfActivationsEquality = solver.makeConstraint(0, 0, "sumOfActivationsEquality"); //Somma_di_attivazioni - ∑a_i =0
        sumOfActivationsEquality.setCoefficient(sumOfActivationsValue, 1);
        for (String instance : previousWeights.keySet()){
            MPVariable activation = solver.makeIntVar(0, 1, "activation_ " + instance);
            activations.put(instance, activation);
            sumOfActivationsEquality.setCoefficient(activation, -1);
        }
        //objective.setCoefficient(sumOfActivationsValue, -1);


        for(String instance : previousWeights.keySet()){
            double instanceAvgRespTime = instanceResponseTimes.get(instance);
            double instanceAvailability = instanceAvailabilities.get(instance);
            double k_i = instanceAvailability/instanceAvgRespTime;
            performanceRatio.put(instance, k_i/k_s);
            MPVariable weight = solver.makeNumVar(0, 1, "weight_ " + instance);
            weights.put(instance, weight);
            objective.setCoefficient(weight, 1/performanceRatio.get(instance));
            objective.setCoefficient(activations.get(instance), -performanceRatio.get(instance));
        }

        MPConstraint sumOfWeights = solver.makeConstraint(1, 1, "sumOfWeights");
        //Sum of weights and activation constraints
        for (String instanceId: weights.keySet()) {
            sumOfWeights.setCoefficient(weights.get(instanceId), 1); //∑w_i = 1

            MPConstraint lowerBoundConstraint = solver.makeConstraint(0, Double.POSITIVE_INFINITY, instanceId + "lowerBoundConstraint"); //w_i - a_i*soglia >= 0
            lowerBoundConstraint.setCoefficient(weights.get(instanceId), 1);
            lowerBoundConstraint.setCoefficient(activations.get(instanceId), -soglia);

            MPConstraint upperBoundConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0, instanceId + "upperBoundConstraint"); //w_i - a_i<=0
            upperBoundConstraint.setCoefficient(weights.get(instanceId), 1);
            upperBoundConstraint.setCoefficient(activations.get(instanceId), -1);
        }

        for(String instance_i: weights.keySet()){
            double instanceAvgRespTime = instanceResponseTimes.get(instance_i);
            double instanceAvailability = instanceAvailabilities.get(instance_i);
            double k_i = instanceAvailability/instanceAvgRespTime;
            //double sumOfWeightsMinusP_i = previousWeights.values().stream().reduce(0.0, Double::sum) - previousWeights.get(instance_i);

            //log.warn("Performance ratio of  " + instance_i + " is " + performanceRatio.get(instance_i));
            double upperBound = performanceRatio.get(instance_i);
            MPConstraint firstConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, upperBound, instance_i + "firstConstraint"); // w_i<= p_i * k_i/k_s + soglia * (n - ∑a_i)
            firstConstraint.setCoefficient(weights.get(instance_i), 1);
            //firstConstraint.setCoefficient(sumOfActivationsValue, soglia);


            for(String instance_j : weights.keySet()){
                if(!instance_i.equals(instance_j)){
                    firstConstraint.setCoefficient(activations.get(instance_j), performanceRatio.get(instance_i) * previousWeights.get(instance_j));
                    double instanceAvgRespTime_j = instanceResponseTimes.get(instance_j);
                    double instanceAvailability_j = instanceAvailabilities.get(instance_j);
                    //firstConstraint.setCoefficient(activations.get(instance2), soglia);

                    double k_j = instanceAvailability_j /instanceAvgRespTime_j;
                    double z_ij = k_i/k_j;

                    if(k_i>=k_j){
                        MPConstraint maxGrowthUpperBoundConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 1, instance_i + "maxGrowthUpperBoundConstraint"); //w_i - k_i/k_j * w_j + a_j  <= 1
                        maxGrowthUpperBoundConstraint.setCoefficient(weights.get(instance_i), 1);
                        maxGrowthUpperBoundConstraint.setCoefficient(weights.get(instance_j), -z_ij);
                        maxGrowthUpperBoundConstraint.setCoefficient(activations.get(instance_j), 1);
                    }
                    /*else{//k_i<k_j il costraint viene implicato da sopra

                        MPConstraint maxDecreaseUpperBoundConstraint = solver.makeConstraint(-1, Double.POSITIVE_INFINITY, instance_i + "maxDecreaseUpperBoundConstraint"); //w_i - k_i/k_j * w_j - a_i  >= -1
                        maxDecreaseUpperBoundConstraint.setCoefficient(weights.get(instance_i), 1);
                        maxDecreaseUpperBoundConstraint.setCoefficient(weights.get(instance_j), -z_ij);
                        maxDecreaseUpperBoundConstraint.setCoefficient(activations.get(instance_i), -1);
                    }

                     */
                }
            }

        }


        objective.setMinimization();



        final MPSolver.ResultStatus resultStatus = solver.solve();
        int numOfActiveInstances = 0;

        for(String instance : activations.keySet()){
            if(activations.get(instance).solutionValue() == 1){
                numOfActiveInstances++;
            }
        }

        if (numOfActiveInstances == 1 || resultStatus != MPSolver.ResultStatus.OPTIMAL && resultStatus != MPSolver.ResultStatus.FEASIBLE && resultStatus != MPSolver.ResultStatus.UNBOUNDED) {
            log.error("The problem does not have a feasible solution!\n");
            for (String instanceId : weights.keySet()) {
                log.info(instanceId + " = " + weights.get(instanceId).solutionValue() + " activation: " + activations.get(instanceId).name() + " = " + activations.get(instanceId).solutionValue());
                //newWeights.put(instanceId, weights.get(instanceId).solutionValue());
            }

            for(String instance : previousWeights.keySet()){
                log.info("Weights before optimization  " + instance + ": " + previousWeights.get(instance));
            }
            log.info("Soglia: " + soglia);

            for(String instance : previousWeights.keySet()){
                log.info("Instance response time " + instance + ":  " + instanceResponseTimes.get(instance));
            }
            log.info("Service response time: " + serviceAvgRespTime);


            for(String instance : previousWeights.keySet()){
                log.info("Instance availability " + instance + ":  " + instanceAvailabilities.get(instance));
            }
            log.info("Service availability: " + serviceAvailability);



            for(String instance : previousWeights.keySet()){
                log.info("K_i of  " + instance + " is " + performanceRatio.get(instance)*k_s);
            }

            log.info("Service performance ratio: " + k_s);
            for (String instanceId: weights.keySet()) {
                log.info("Instance performance ratio " + instanceId + ": " + performanceRatio.get(instanceId));
            }

            return null;
        }

        //log.info("Solution:");
        //log.info("Objective value = " + objective.value());

        for (String instanceId : weights.keySet()) {
            //log.info(instanceId + " = " + weights.get(instanceId).solutionValue() + " activation: " + activations.get(instanceId).name() + " = " + activations.get(instanceId).solutionValue());
            newWeights.put(instanceId, weights.get(instanceId).solutionValue());
        }
        return newWeights;
    }
}

