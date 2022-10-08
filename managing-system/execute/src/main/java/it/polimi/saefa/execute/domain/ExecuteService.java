package it.polimi.saefa.execute.domain;

import it.polimi.saefa.configparser.CustomPropertiesWriter;
import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j
@org.springframework.stereotype.Service
public class ExecuteService {

    @Autowired
    private KnowledgeClient knowledgeClient;
    @Autowired
    private MonitorClient monitorClient;
    @Autowired
    private ConfigManagerClient configManagerClient;
    @Autowired
    private InstancesManagerClient instancesManagerClient;


    public void execute() {
        try {
            log.info("Starting Execute step");
            knowledgeClient.notifyModuleStart(Modules.EXECUTE);
            List<AdaptationOption> chosenAdaptationOptions = knowledgeClient.getChosenAdaptationOptions();
            chosenAdaptationOptions.forEach(adaptationOption -> {
                log.info("Executing adaptation option: " + adaptationOption.getDescription());
                Class<? extends AdaptationOption> clazz = adaptationOption.getClass();
                if (clazz.equals(AddInstances.class)) {
                    handleAddInstances((AddInstances) (adaptationOption));
                } else if (clazz.equals(RemoveInstance.class)) {
                    handleRemoveInstanceOption((RemoveInstance) (adaptationOption));
                } else if (clazz.equals(ChangeLoadBalancerWeights.class)) {
                    handleChangeLBWeights((ChangeLoadBalancerWeights) (adaptationOption));
                } else {
                    log.error("Unknown adaptation option type: " + adaptationOption.getClass());
                }
            });
            log.info("Ending execute. Notifying Monitor module to continue the loop.");
            monitorClient.notifyFinishedIteration();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleAddInstances(AddInstances addInstancesOption) {
        String serviceId = addInstancesOption.getServiceId();
        Service service = knowledgeClient.getServicesMap().get(serviceId);
        if (!service.getCurrentImplementationId().equals(addInstancesOption.getServiceImplementationId()))
            throw new RuntimeException("Service implementation id mismatch. Expected: " + service.getCurrentImplementationId() + " Actual: " + addInstancesOption.getServiceImplementationId());
        StartNewInstancesResponse instancesResponse = instancesManagerClient.addInstances(new StartNewInstancesRequest(addInstancesOption.getServiceImplementationId(), addInstancesOption.getNumberOfInstancesToAdd()));

        if (instancesResponse.getDockerizedInstances().isEmpty())
            throw new RuntimeException("No instances were added");

        Set<String> newInstancesIds = new HashSet<>();
        instancesResponse.getDockerizedInstances().forEach(instance -> {
            log.info("Adding instance to service" + addInstancesOption.getServiceId() + " with new instance " + instance.getAddress());
            newInstancesIds.add(service.createInstance(instance.getAddress()).getInstanceId());
        });

        //todo vedere se usare il nuovo metodo redistribute weights

        int newNumberOfInstances = service.getInstances().size();
        int oldNumberOfInstances = newNumberOfInstances - instancesResponse.getDockerizedInstances().size();

        for (Instance instance : service.getInstances()) {
            if (newInstancesIds.contains(instance.getInstanceId())) {
                service.setLoadBalancerWeight(instance, 1.0 / newNumberOfInstances);
            } else {
                service.setLoadBalancerWeight(instance, service.getLoadBalancerWeight(instance) * oldNumberOfInstances / newNumberOfInstances);
            }
        }
        updateLoadbalancerWeights(service.getServiceId(), service.getLoadBalancerWeights());
        knowledgeClient.updateService(service);
    }

    private void handleRemoveInstanceOption(RemoveInstance removeInstancesOption) {
        String serviceId = removeInstancesOption.getServiceId();
        Service service = knowledgeClient.getService(serviceId);
        Map<String, Double> newWeights = redistributeWeight(service.getLoadBalancerWeights(), removeInstancesOption.getInstanceId());
        removeInstance(serviceId, removeInstancesOption.getInstanceId());
        updateLoadbalancerWeights(service.getServiceId(), newWeights);
    }

    private void handleChangeLBWeights(ChangeLoadBalancerWeights changeLoadBalancerWeightsOption) {
        String serviceId = changeLoadBalancerWeightsOption.getServiceId();
        Map<String, Double> newWeights = changeLoadBalancerWeightsOption.getNewWeights();
        newWeights.forEach((instanceId, weight) -> {
            if (weight == 0.0)
                removeInstance(serviceId, instanceId);
        });
        updateLoadbalancerWeights(serviceId, newWeights);
    }


    /**
     * Removes an instance from a service.
     * Contacts the instanceManager actuator to shut down the instance.
     * Then, it notifies the knowledge.
     *
     * @param serviceId
     * @param instanceToRemoveId
     */
    private void removeInstance(String serviceId, String instanceToRemoveId) {
        String[] ipPort = instanceToRemoveId.split("@")[1].split(":");
        instancesManagerClient.removeInstance(new RemoveInstanceRequest(instanceToRemoveId.split("@")[0], ipPort[0], Integer.parseInt(ipPort[1])));
        knowledgeClient.notifyShutdownInstance(Map.of("serviceId", serviceId, "instanceId", instanceToRemoveId));
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
     * Contacts the config manager to change the weights of the load balancer of the specified service.
     * Then, it updates the weights in the knowledge.
     *
     * @param serviceId
     * @param weights
     */
    private void updateLoadbalancerWeights(String serviceId, Map<String, Double> weights) {
        List<PropertyToChange> propertyToChangeList = new LinkedList<>();
        List<String> weightsToRemove = new LinkedList<>();
        weights.forEach((instanceId, weight) -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            if (weight != 0.0) {
                propertyToChangeList.add(new PropertyToChange(null, propertyKey, weight.toString()));
            } else {
                propertyToChangeList.add(new PropertyToChange(null, propertyKey));
                weightsToRemove.add(instanceId);
            }
        });
        weightsToRemove.forEach(weights::remove);
        configManagerClient.changeProperty(new ChangePropertyRequest(propertyToChangeList));
        knowledgeClient.setLoadBalancerWeights(serviceId, weights);
    }


    public void breakpoint(){
        log.info("breakpoint");
    }
}
