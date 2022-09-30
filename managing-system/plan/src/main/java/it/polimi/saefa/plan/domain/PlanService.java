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
    private Map<String, Service> servicesMap;

    public void startPlan() {
        Map<String, List<AdaptationOption>> adaptationOptions = knowledgeClient.getProposedAdaptationOptions();
        servicesMap = knowledgeClient.getServicesMap();

        for (String serviceId : adaptationOptions.keySet()) {
            List<AdaptationOption> options = adaptationOptions.get(serviceId);
            for (AdaptationOption option : options) {
                log.info("Adaptation option: {}", option.getDescription());
                if(option.getClass().equals(ChangeLoadBalancerWeights.class)){
                    ChangeLoadBalancerWeights changeLoadBalancerWeights = (ChangeLoadBalancerWeights) option;
                    changeLoadBalancerWeights.setNewWeights(handleChangeLoadBalancerWeights(changeLoadBalancerWeights, servicesMap.get(option.getServiceId())));
                }



            }


        }
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

        for(Instance instance : servicesMap.get(service.getServiceId()).getInstances()){
            if(emptyWeights)
                previousWeights.put(instance.getAddress(), defaultWeight);
            double serviceAvgRespTime = service.getCurrentImplementationObject().getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
            double instanceAvgRespTime = instance.getAdaptationParamCollection().getLatestAdaptationParamValue(AverageResponseTime.class).getValue();
            double serviceAvgAvailability = option.getServiceAverageAvailability();
            double instanceAvailability = instance.getAdaptationParamCollection().getLatestAdaptationParamValue(Availability.class).getValue();

            double upperBound = previousWeights.get(instance.getAddress()) * (serviceAvgRespTime/instanceAvgRespTime) * (instanceAvailability/serviceAvgAvailability);
            MPVariable weight = solver.makeNumVar(0, upperBound, instance.getInstanceId());
            weights.put(instance.getInstanceId(), weight);

            MPConstraint increaseDecreaseTimeConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, previousWeights.get(instance.getAddress())*(instanceAvgRespTime-serviceAvgRespTime), "increaseDecreaseTimeConstraint");
            increaseDecreaseTimeConstraint.setCoefficient(weight, instanceAvgRespTime-serviceAvgRespTime);
            //increaseDecreaseConstraintList.add(increaseDecreaseTimeConstraint);

            MPConstraint increaseDecreaseAvailabilityConstraint = solver.makeConstraint(previousWeights.get(instance.getAddress())*(instanceAvailability-serviceAvgAvailability), Double.POSITIVE_INFINITY, "increaseDecreaseAvailabilityConstraint");
            increaseDecreaseAvailabilityConstraint.setCoefficient(weight, instanceAvailability-serviceAvgAvailability);
            //increaseDecreaseConstraintList.add(increaseDecreaseAvailabilityConstraint);

            objective.setCoefficient(weight, instanceAvgRespTime/instanceAvailability);
        }

        MPConstraint sumOfWeights = solver.makeConstraint(1, 1, "sumOfWeights");
        for (MPVariable weight : weights.values()) {
            sumOfWeights.setCoefficient(weight, 1);
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

    static {
        log.debug("Current directory: {}", System.getProperty("user.dir"));
        try {
            System.load(ResourceUtils.getFile("classpath:libjniortools.dylib").getAbsolutePath());
            System.load(ResourceUtils.getFile("classpath:libortools.9.dylib").getAbsolutePath());
        } catch (Exception e) {
            log.error("Error loading or-tools libraries", e);
        }
    }


    public Map<String, Double> handleChangeLoadBalancerWeightsTEST() {
        log.warn("TEST");
        Map<String, Double> previousWeights = new HashMap<>();
        previousWeights.put("1", 0.34);
        previousWeights.put("2", 0.33);
        previousWeights.put("3", 0.33);

        Map<String, Double> newWeights = new HashMap<>();

        MPSolver solver = MPSolver.createSolver("GLOP");
        Map<String, MPVariable> weights = new HashMap<>();
        //List<MPConstraint> increaseDecreaseConstraintList = new LinkedList<>();
        MPObjective objective = solver.objective();

        for(String instance : previousWeights.keySet()){
            double serviceAvgRespTime = 5;
            double serviceAvailability = 0.5;
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
            double upperBound = previousWeights.get(instance) * (serviceAvgRespTime/instanceAvgRespTime) * (instanceAvailability/serviceAvailability);
            upperBound = previousWeights.get(instance) * 2;
            MPVariable weight = solver.makeNumVar(0, upperBound, instance);
            weights.put(instance, weight);

            MPConstraint increaseDecreaseTimeConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, previousWeights.get(instance)*(instanceAvgRespTime-serviceAvgRespTime), "increaseDecreaseTimeConstraint");
            increaseDecreaseTimeConstraint.setCoefficient(weight, instanceAvgRespTime-serviceAvgRespTime);
            //increaseDecreaseConstraintList.add(increaseDecreaseTimeConstraint);

            MPConstraint increaseDecreaseAvailabilityConstraint = solver.makeConstraint(previousWeights.get(instance)*(instanceAvailability-serviceAvailability), Double.POSITIVE_INFINITY, "increaseDecreaseAvailabilityConstraint");
            increaseDecreaseAvailabilityConstraint.setCoefficient(weight, instanceAvailability-serviceAvailability);
            //increaseDecreaseConstraintList.add(increaseDecreaseAvailabilityConstraint);

            objective.setCoefficient(weight, instanceAvgRespTime/instanceAvailability);
        }


        MPConstraint sumOfWeights = solver.makeConstraint(1, 1, "sumOfWeights");
        for (MPVariable weight : weights.values()) {
            sumOfWeights.setCoefficient(weight, 1);
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

