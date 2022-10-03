package it.polimi.saefa.plan.domain;

import com.google.ortools.linearsolver.*;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.options.ChangeLoadBalancerWeights;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.plan.externalInterfaces.ExecuteClient;
import it.polimi.saefa.plan.externalInterfaces.KnowledgeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@org.springframework.stereotype.Service
public class PlanService {

    @Autowired
    private KnowledgeClient knowledgeClient;

    @Autowired
    private ExecuteClient executeClient;

    private Map<String, Service> servicesMap;
    private Map<String, AdaptationOption> chosenAdaptationOptions;

    static {
        log.debug("Current directory: {}", System.getProperty("user.dir"));
        try {
            System.load(ResourceUtils.getFile("classpath:libjniortools.dylib").getAbsolutePath());
            System.load(ResourceUtils.getFile("classpath:libortools.9.dylib").getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Error loading or-tools libraries", e);
        }
    }

    public void startPlan() {
        Map<String, List<AdaptationOption>> proposedAdaptationOptions = knowledgeClient.getProposedAdaptationOptions();
        Map<String, AdaptationOption> chosenAdaptationOptionMap = new HashMap<>();
        servicesMap = knowledgeClient.getServicesMap();

        for (String serviceId : proposedAdaptationOptions.keySet()) {
            List<AdaptationOption> options = proposedAdaptationOptions.get(serviceId);
            for (AdaptationOption option : options) {
                log.info("Adaptation option: {}", option.getDescription());
                if(option.getClass().equals(ChangeLoadBalancerWeights.class)){
                    ChangeLoadBalancerWeights changeLoadBalancerWeights = (ChangeLoadBalancerWeights) option;
                    changeLoadBalancerWeights.setNewWeights(handleChangeLoadBalancerWeights(changeLoadBalancerWeights, servicesMap.get(option.getServiceId())));
                    chosenAdaptationOptionMap.put(serviceId, changeLoadBalancerWeights); //TODO REMOVE IN FUTURO, solo per test
                }
            }
        }
        knowledgeClient.chooseAdaptationOptions(chosenAdaptationOptionMap.values().stream().toList());
        executeClient.start();
    }

    public Map<String, Double> handleChangeLoadBalancerWeights(ChangeLoadBalancerWeights option, Service service) {
        Map<String, Double> previousWeights = service.getConfiguration().getLoadBalancerWeights();
        double shutdownThreshold = service.getCurrentImplementationObject().getInstanceLoadShutdownThreshold() / service.getInstances().size();
        double defaultWeight = 1.0 / service.getInstances().size();
        boolean emptyWeights = previousWeights.isEmpty();

        Map<String, Double> newWeights = new HashMap<>();

        MPSolver solver = MPSolver.createSolver("SCIP");
        Map<String, MPVariable> weights = new HashMap<>();
        Map<String, MPVariable> activations = new HashMap<>();
        MPObjective objective = solver.objective();// min{∑(w_i/z_i) - ∑(a_i * z_i)}
        MPConstraint sumOfWeights = solver.makeConstraint(1, 1, "sumOfWeights"); //∑w_i = 1

        double serviceAvgRespTime = service.getCurrentImplementationObject().getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
        double serviceAvgAvailability = option.getServiceAverageAvailability(); //TODO se l'availability del sistema è quella media e non in parallelo, va tolta dall'adaptation option e presa dal servizio
        double k_s = serviceAvgAvailability/serviceAvgRespTime; //performance indicator

        for(Instance instance : service.getInstances()){
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

            if(emptyWeights)
                previousWeights.put(instance.getInstanceId(), defaultWeight);
            double instanceAvgRespTime = instance.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
            double instanceAvailability = instance.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();
            double k_i = instanceAvailability/instanceAvgRespTime;
            double z_i = k_i/k_s;

            objective.setCoefficient(weight, 1/z_i);
            objective.setCoefficient(activation, -z_i);
        }

        for (Instance instance_i : service.getInstances()) {
            MPVariable weight_i = weights.get(instance_i.getInstanceId());
            double instanceAvgRespTime_i = instance_i.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
            double instanceAvailability_i = instance_i.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();
            double k_i = instanceAvailability_i/instanceAvgRespTime_i;

            double z_i = k_i/k_s;
            MPConstraint firstConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, z_i, instance_i + "firstConstraint"); // w_i<= p_i * k_i/k_s + soglia * (n - ∑a_i)
            firstConstraint.setCoefficient(weight_i, 1);

            for(Instance instance_j : service.getInstances()){
                if(!instance_i.getInstanceId().equals(instance_j.getInstanceId())){
                    MPVariable weight_j = weights.get(instance_j.getInstanceId());
                    double instanceAvgRespTime_j = instance_j.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
                    double instanceAvailability_j = instance_j.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();
                    firstConstraint.setCoefficient(activations.get(instance_j.getInstanceId()), z_i * previousWeights.get(instance_j.getInstanceId()));

                    double k_j = instanceAvailability_j /instanceAvgRespTime_j;
                    double z_ij = k_i/k_j;

                    if(k_i>=k_j){
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
        }

        objective.setMinimization();
        final MPSolver.ResultStatus resultStatus = solver.solve();

        if (resultStatus != MPSolver.ResultStatus.OPTIMAL && resultStatus != MPSolver.ResultStatus.FEASIBLE && resultStatus != MPSolver.ResultStatus.UNBOUNDED) {
            log.error("The problem of determining new weights for service " + service.getServiceId() + " does not have an optimal solution!");
            return null;
        }

        log.debug("Solution:");
        log.debug("Objective value = " + objective.value());

        for (String instanceId : weights.keySet()) {
            log.debug(instanceId + " weight = " + weights.get(instanceId).solutionValue());
            newWeights.put(instanceId, weights.get(instanceId).solutionValue());
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
        for(String instance : previousWeights.keySet()){
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

