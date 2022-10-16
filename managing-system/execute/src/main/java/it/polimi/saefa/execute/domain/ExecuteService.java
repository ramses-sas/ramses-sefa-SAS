package it.polimi.saefa.execute.domain;

import it.polimi.saefa.configparser.CustomPropertiesWriter;
import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceImplementation;
import it.polimi.saefa.knowledge.rest.AddInstanceRequest;
import it.polimi.saefa.knowledge.rest.ChangeOfImplementationRequest;
import it.polimi.saefa.knowledge.rest.ShutdownInstanceRequest;
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
                for(AdaptationOption adaptationOption : adaptationOptions){
                    log.info("Executing adaptation option: " + adaptationOption.getDescription());
                    Class<? extends AdaptationOption> clazz = adaptationOption.getClass();
                    if (AddInstanceOption.class.equals(clazz)) {
                        handleAddInstanceOption((AddInstanceOption) (adaptationOption));
                    } else if (RemoveInstanceOption.class.equals(clazz)) {
                        handleRemoveInstanceOption((RemoveInstanceOption) (adaptationOption));
                    } else if (ChangeLoadBalancerWeightsOption.class.equals(clazz)) {
                        handleChangeLBWeightsOption((ChangeLoadBalancerWeightsOption) (adaptationOption));
                    } else if(ChangeImplementationOption.class.equals(clazz)){
                        handleChangeImplementationOption((ChangeImplementationOption) (adaptationOption));
                    } else {
                        log.error("Unknown adaptation option type: " + adaptationOption.getClass());
                    }
                }
            });
            log.info("Ending execute. Notifying Monitor module to continue the loop.");
            monitorClient.notifyFinishedIteration();
        } catch (Exception e) {
            knowledgeClient.setFailedModule(Modules.EXECUTE);
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void handleChangeImplementationOption(ChangeImplementationOption changeImplementationOption) {
        String serviceId = changeImplementationOption.getServiceId();
        Service service = knowledgeClient.getService(serviceId);
        ServiceImplementation oldImplementation = service.getCurrentImplementation();

        StartNewInstancesResponse instancesResponse = instancesManagerClient.addInstances(new StartNewInstancesRequest(changeImplementationOption.getNewImplementationId(), changeImplementationOption.getNumberOfInstances()));

        List<StartNewInstancesResponse.SingleInstanceResponse> newInstances = instancesResponse.getDockerizedInstances();
        if (newInstances.isEmpty())
            throw new RuntimeException("No instances were added");

        List<String> newInstancesAddresses = newInstances.stream().collect(LinkedList::new, (list, instance) -> list.add(instance.getAddress()), List::addAll);

        List<String> oldInstancesIds = oldImplementation.getInstances().values().stream().collect(LinkedList::new, (list, instance) -> list.add(instance.getInstanceId()), List::addAll);
        for(String instanceId : oldInstancesIds){
            removeInstance(instanceId);
        }
        removeLoadBalancerWeights(serviceId, oldInstancesIds);
        knowledgeClient.notifyChangeOfImplementation(new ChangeOfImplementationRequest(serviceId, changeImplementationOption.getNewImplementationId(), newInstancesAddresses));

    }

    private void handleAddInstanceOption(AddInstanceOption addInstanceOption) {
        String serviceId = addInstanceOption.getServiceId();
        Service service = knowledgeClient.getService(serviceId);
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
        knowledgeClient.notifyAddInstance(new AddInstanceRequest(addInstanceOption.getServiceId(), newInstanceId));
        if (newWeights != null) {
            for(String instanceId : newWeights.keySet()){
                if(newWeights.get(instanceId) == 0.0){
                    knowledgeClient.notifyShutdownInstance(new ShutdownInstanceRequest(serviceId, removeInstance(instanceId)));
                }
            }
            knowledgeClient.setLoadBalancerWeights(serviceId, updateConfigLoadbalancerWeights(service.getServiceId(), newWeights));
        }
    }

    private void handleRemoveInstanceOption(RemoveInstanceOption removeInstancesOptionOption) {
        String serviceId = removeInstancesOptionOption.getServiceId();
        knowledgeClient.notifyShutdownInstance(new ShutdownInstanceRequest(serviceId, removeInstance(removeInstancesOptionOption.getInstanceId())));
        if(removeInstancesOptionOption.getNewWeights()!=null) {
            knowledgeClient.setLoadBalancerWeights(serviceId, updateConfigLoadbalancerWeights(serviceId, removeInstancesOptionOption.getNewWeights()));
        }
    }

    private void handleChangeLBWeightsOption(ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption) {
        String serviceId = changeLoadBalancerWeightsOption.getServiceId();
        Map<String, Double> newWeights = changeLoadBalancerWeightsOption.getNewWeights();
        newWeights.forEach((instanceId, weight) -> {
            if (weight == 0.0)
                knowledgeClient.notifyShutdownInstance(new ShutdownInstanceRequest(serviceId, removeInstance(instanceId)));
        });
        knowledgeClient.setLoadBalancerWeights(serviceId, updateConfigLoadbalancerWeights(serviceId, newWeights));
    }


    /**
     * Removes an instance from a service.
     * Contacts the instanceManager actuator to shut down the instance.
     * Then, it notifies the knowledge.
     *
     * @param instanceToRemoveId
     *
     * @return the instanceId of the removed instance
     */
    private String removeInstance(String instanceToRemoveId) {
        String[] ipPort = instanceToRemoveId.split("@")[1].split(":");
        instancesManagerClient.removeInstance(new RemoveInstanceRequest(instanceToRemoveId.split("@")[0], ipPort[0], Integer.parseInt(ipPort[1])));
        return instanceToRemoveId;
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
     * Contacts the config manager to change the weights of the load balancer of the specified service. It also removes
     * the weights of the instances that will be shutdown.
     *
     * @param serviceId
     * @param weights
     * @return the new weights map
     */
    private Map<String, Double> updateConfigLoadbalancerWeights(String serviceId, Map<String, Double> weights) {
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
        return weights;
    }

    private void removeLoadBalancerWeights(String serviceId, List<String> instanceIds) {
        List<PropertyToChange> propertyToChangeList = new LinkedList<>();
        instanceIds.forEach(instanceId -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            propertyToChangeList.add(new PropertyToChange(null, propertyKey));
        });
        configManagerClient.changeProperty(new ChangePropertyRequest(propertyToChangeList));
    }


    public void breakpoint(){
        log.info("breakpoint");
    }
}
