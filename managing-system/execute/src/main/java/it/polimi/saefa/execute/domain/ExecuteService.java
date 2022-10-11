package it.polimi.saefa.execute.domain;

import it.polimi.saefa.configparser.CustomPropertiesWriter;
import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
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
            Map<String, List<AdaptationOption>> chosenAdaptationOptions = knowledgeClient.getChosenAdaptationOptions();
            chosenAdaptationOptions.values().forEach(adaptationOptions -> {
                adaptationOptions.forEach(adaptationOption -> {
                    log.info("Executing adaptation option: " + adaptationOption.getDescription());
                    Class<? extends AdaptationOption> clazz = adaptationOption.getClass();
                    if (clazz.equals(AddInstance.class)) {
                        //handleAddInstance((AddInstance) (adaptationOption));
                    } else if (clazz.equals(RemoveInstance.class)) {
                        //handleRemoveInstanceOption((RemoveInstance) (adaptationOption));
                    } else if (clazz.equals(ChangeLoadBalancerWeights.class)) {
                        //handleChangeLBWeights((ChangeLoadBalancerWeights) (adaptationOption));
                    } else {
                        log.error("Unknown adaptation option type: " + adaptationOption.getClass());
                    }
                });
            });
            log.info("Ending execute. Notifying Monitor module to continue the loop.");
            monitorClient.notifyFinishedIteration();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleAddInstance(AddInstance addInstanceOption) {
        String serviceId = addInstanceOption.getServiceId();
        Service service = knowledgeClient.getServicesMap().get(serviceId);
        if (!service.getCurrentImplementationId().equals(addInstanceOption.getServiceImplementationId()))
            throw new RuntimeException("Service implementation id mismatch. Expected: " + service.getCurrentImplementationId() + " Actual: " + addInstanceOption.getServiceImplementationId());
        StartNewInstancesResponse instancesResponse = instancesManagerClient.addInstances(new StartNewInstancesRequest(addInstanceOption.getServiceImplementationId(), 1));

        if (instancesResponse.getDockerizedInstances().isEmpty())
            throw new RuntimeException("No instances were added");
        if (instancesResponse.getDockerizedInstances().size() > 1)
            throw new RuntimeException("More than one instance was added");

        String newInstancesAddress = instancesResponse.getDockerizedInstances().get(0).getAddress();
        String newInstanceId = service.createInstance(newInstancesAddress).getInstanceId();
        log.info("Adding instance to service" + addInstanceOption.getServiceId() + " with new instance " + newInstanceId);
        Map<String, Double> newWeights = addInstanceOption.getFinalWeights(newInstanceId);
        if (newWeights != null)
            updateLoadbalancerWeights(service.getServiceId(), newWeights);
        knowledgeClient.updateService(service);
    }

    private void handleRemoveInstanceOption(RemoveInstance removeInstancesOption) {
        String serviceId = removeInstancesOption.getServiceId();
        removeInstance(serviceId, removeInstancesOption.getInstanceId());
        if(removeInstancesOption.getNewWeights()!=null) {
            updateLoadbalancerWeights(serviceId, removeInstancesOption.getNewWeights());
        }
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
