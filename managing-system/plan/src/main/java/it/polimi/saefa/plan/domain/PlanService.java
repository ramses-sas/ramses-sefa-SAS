package it.polimi.saefa.plan.domain;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
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
                    chosenAdaptationOptionMap.put(serviceId, changeLoadBalancerWeights); //TODO REMOVE IN FUTURO
                }
            }
        }
        knowledgeClient.chooseAdaptationOptions(chosenAdaptationOptionMap.values().stream().toList());
        executeClient.start();
    }

    public Map<String, Double> handleChangeLoadBalancerWeights(ChangeLoadBalancerWeights option, Service service) {
        Map<String, Double> previousWeights = service.getConfiguration().getLoadBalancerWeights();
        double defaultWeight = 1.0 / service.getInstances().size();
        boolean emptyWeights = previousWeights.isEmpty();

        Map<String, Double> newWeights = new HashMap<>();

        MPSolver solver = MPSolver.createSolver("GLOP");
        Map<String, MPVariable> weights = new HashMap<>();
        //List<MPConstraint> increaseDecreaseConstraintList = new LinkedList<>();
        MPObjective objective = solver.objective();
        MPConstraint sumOfWeights = solver.makeConstraint(1, 1, "sumOfWeights");


        double serviceAvgRespTime = service.getCurrentImplementationObject().getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
        double serviceAvgAvailability = option.getServiceAverageAvailability();

        double k_s = serviceAvgAvailability/serviceAvgRespTime; //performance indicator

        for(Instance instance : servicesMap.get(service.getServiceId()).getInstances()){
            if(emptyWeights)
                previousWeights.put(instance.getAddress(), defaultWeight);
            double instanceAvgRespTime = instance.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
            double instanceAvailability = instance.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();
            double k_i = instanceAvailability/instanceAvgRespTime;
            double upperBound = previousWeights.get(instance.getAddress()) * (k_i/k_s); // k_i/k_s is the performance ratio between instance and service
            /* performanceRatioConstraint: weight_i <= old_weight_i * k_i/k_s */
            MPVariable weight = solver.makeNumVar(0, upperBound, instance.getInstanceId());

            weights.put(instance.getInstanceId(), weight);
            sumOfWeights.setCoefficient(weight, 1);
            objective.setCoefficient(weight, instanceAvgRespTime/instanceAvailability);
        }

        for (Instance instance_i : servicesMap.get(service.getServiceId()).getInstances()) {
            MPVariable weight_i = weights.get(instance_i.getInstanceId());
            double instanceAvgRespTime_i = instance_i.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
            double instanceAvailability_i = instance_i.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();
            double k_i = instanceAvailability_i/instanceAvgRespTime_i;

            for(Instance instance_j : servicesMap.get(service.getServiceId()).getInstances()){
                if(!instance_i.getInstanceId().equals(instance_j.getInstanceId())){
                    MPVariable weight_j = weights.get(instance_j.getInstanceId());
                    double instanceAvgRespTime_j = instance_j.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
                    double instanceAvailability_j = instance_j.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();
                    double k_j = instanceAvailability_j/instanceAvgRespTime_j;

                    /* weightRatioConstraint: weight_i <= k_i/k_j * weight_j */
                    MPConstraint weightRatioConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0, "weightRatioConstraint between " + instance_i.getInstanceId() + " and " + instance_j.getInstanceId());
                    weightRatioConstraint.setCoefficient(weight_i, k_j);
                    weightRatioConstraint.setCoefficient(weight_j, -k_i);
                }
            }
        }
        objective.setMinimization();
        solver.solve();
        log.info("Solution:");
        log.info("Objective value = " + objective.value());

        for (String instanceId : weights.keySet()) {
            log.info(instanceId + " = " + weights.get(instanceId).solutionValue());
            newWeights.put(instanceId, weights.get(instanceId).solutionValue());
        }
        return newWeights;
    }

    public Map<String, Double> handleChangeLoadBalancerWeightsTEST() {
        log.warn("TEST");
        Map<String, Double> previousWeights = new HashMap<>();
        previousWeights.put("1", 0.3);
        previousWeights.put("2", 0.25);
        previousWeights.put("3", 0.3);
        previousWeights.put("4", 0.15);

        Map<String, Double> newWeights = new HashMap<>();

        MPSolver solver = MPSolver.createSolver("GLOP");
        Map<String, MPVariable> weights = new HashMap<>();
        //List<MPConstraint> increaseDecreaseConstraintList = new LinkedList<>();
        MPObjective objective = solver.objective();

        for(String instance : previousWeights.keySet()){
            double serviceAvgRespTime = 4.25;
            double serviceAvailability = 0.675;
            double instanceAvgRespTime = 9;
            double instanceAvailability = 0.3;


            if(instance.equals("1")) {
                instanceAvgRespTime = 1.51;
                instanceAvailability = 0.75;
            }

            if (instance.equals("2")) {
                instanceAvgRespTime = 9;
                instanceAvailability = 0.3;
            }

            if(instance.equals("3")) {
                instanceAvgRespTime = 1.5;
                instanceAvailability = 0.75;
            }

            if(instance.equals("4")) {
                instanceAvgRespTime = 5;
                instanceAvailability = 0.9;
            }

            double upperBound = previousWeights.get(instance) * (serviceAvgRespTime/instanceAvgRespTime) * (instanceAvailability/serviceAvailability);
            upperBound = previousWeights.get(instance) * 2;
            MPVariable weight = solver.makeNumVar(0, upperBound, instance);
            weights.put(instance, weight);



            objective.setCoefficient(weight, instanceAvgRespTime/instanceAvailability);
        }


        MPConstraint sumOfWeights = solver.makeConstraint(1, 1, "sumOfWeights");
        for (MPVariable weight : weights.values()) {
            sumOfWeights.setCoefficient(weight, 1);
        }

        for(String instance: weights.keySet()){
            double instanceAvgRespTime = 9;
            double instanceAvailability = 0.3;


            if(instance.equals("1")) {
                instanceAvgRespTime = 1.51;
                instanceAvailability = 0.75;
            }

            if (instance.equals("2")) {
                instanceAvgRespTime = 9;
                instanceAvailability = 0.3;
            }

            if(instance.equals("3")) {
                instanceAvgRespTime = 1.5;
                instanceAvailability = 0.75;
            }

            if(instance.equals("4")) {
                instanceAvgRespTime = 5;
                instanceAvailability = 0.9;
            }

            double k_i = instanceAvailability/instanceAvgRespTime;

            for(String instance2 : weights.keySet()){
                double instanceAvgRespTime2 = 9;
                double instanceAvailability2 = 0.3;
                if(!instance.equals(instance2)){

                    if(instance2.equals("1")) {
                        instanceAvgRespTime2 = 1.51;
                        instanceAvailability2 = 0.75;
                    }

                    if (instance2.equals("2")) {
                        instanceAvgRespTime2 = 9;
                        instanceAvailability2 = 0.3;
                    }

                    if(instance2.equals("3")) {
                        instanceAvgRespTime2 = 1.5;
                        instanceAvailability2 = 0.75;
                    }

                    if(instance2.equals("4")) {
                        instanceAvgRespTime2 = 5;
                        instanceAvailability2 = 0.9;
                    }

                    double k_j = instanceAvailability2/instanceAvgRespTime2;

                    double z_ij = k_i/k_j;


                    MPConstraint constraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0, "upperBoundSimilarWeights");
                    constraint.setCoefficient(weights.get(instance), 1);
                    constraint.setCoefficient(weights.get(instance2), -z_ij);
                }
            }

        }

        objective.setMinimization();

        solver.solve();
        log.info("Solution:");
        log.info("Objective value = " + objective.value());

        for (String instanceId : weights.keySet()) {
            log.info(instanceId + " = " + weights.get(instanceId).solutionValue());
            newWeights.put(instanceId, weights.get(instanceId).solutionValue());
        }
        return newWeights;
    }
}

