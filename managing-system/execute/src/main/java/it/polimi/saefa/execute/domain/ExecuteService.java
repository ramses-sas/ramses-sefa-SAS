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
            chosenAdaptationOptions.forEach((serviceId, serviceAdaptationOptionsList) -> {
                for (AdaptationOption adaptationOption : serviceAdaptationOptionsList) {
                    log.info("Executing adaptation option: " + adaptationOption.getDescription());
                    Class<? extends AdaptationOption> clazz = adaptationOption.getClass();
                    if (AddInstanceOption.class.equals(clazz)) {
                        handleAddInstanceOption((AddInstanceOption) (adaptationOption));
                    } else if (ShutdownInstanceOption.class.equals(clazz)) {
                        handleShutdownInstanceOption((ShutdownInstanceOption) (adaptationOption));
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



    // AdaptationOption-specific handlers

    /** Add an instance according to the given AdaptationOption.
     * Involves updating the knowledge accordingly, and contacting the Config Manager and the Instances Manager actuators.
     *
     * @param addInstanceOption the AdaptationOption defining the instance to add, the instances to shut down and
     *                          the new load balancer weights for the instances.
     */
    private void handleAddInstanceOption(AddInstanceOption addInstanceOption) {
        String serviceId = addInstanceOption.getServiceId();
        Service service = knowledgeClient.getService(serviceId);
        if (!service.getCurrentImplementationId().equals(addInstanceOption.getServiceImplementationId()))
            throw new RuntimeException("Service implementation id mismatch. Expected: " + service.getCurrentImplementationId() + " Actual: " + addInstanceOption.getServiceImplementationId());
        StartNewInstancesResponse instancesResponse = instancesManagerClient.addInstances(new StartNewInstancesRequest(addInstanceOption.getServiceImplementationId(), 1));

        if (instancesResponse.getDockerizedInstances().isEmpty())
            throw new RuntimeException("No instances were added");

        String newInstancesAddress = instancesResponse.getDockerizedInstances().get(0).getAddress() + ":" + instancesResponse.getDockerizedInstances().get(0).getPort();
        String newInstanceId = service.createInstance(newInstancesAddress).getInstanceId();
        log.info("Adding instance to service" + serviceId + " with new instance " + newInstanceId);
        Map<String, Double> newWeights = addInstanceOption.getFinalWeights(newInstanceId);
        knowledgeClient.notifyAddInstance(new AddInstanceRequest(serviceId, newInstancesAddress));
        if (newWeights != null) {
            for (String instanceToShutdownId : addInstanceOption.getInstancesToShutdownIds()) {
                actuatorShutdownInstance(instanceToShutdownId);
                knowledgeClient.notifyShutdownInstance(new ShutdownInstanceRequest(serviceId, instanceToShutdownId));
            }
            actuatorUpdateLoadbalancerWeights(service.getServiceId(), newWeights, addInstanceOption.getInstancesToShutdownIds());
            knowledgeClient.setLoadBalancerWeights(serviceId, newWeights);
        }
    }

    /** Shutdown the instance specified by the given AdaptationOption.
     * Involves updating the knowledge accordingly and contacting the Config Manager and the Instances Manager actuators.
     *
     * @param shutdownInstanceOption the ShutdownInstanceOption defining the new weights and the instance to shut down
     */
    private void handleShutdownInstanceOption(ShutdownInstanceOption shutdownInstanceOption) {
        String serviceId = shutdownInstanceOption.getServiceId();
        String instanceToShutdownId = shutdownInstanceOption.getInstanceToShutdownId();
        Map<String, Double> newWeights = shutdownInstanceOption.getNewWeights();
        actuatorShutdownInstance(instanceToShutdownId);
        knowledgeClient.notifyShutdownInstance(new ShutdownInstanceRequest(serviceId, instanceToShutdownId));
        if (newWeights != null) {
            actuatorUpdateLoadbalancerWeights(serviceId, newWeights, List.of(instanceToShutdownId));
            knowledgeClient.setLoadBalancerWeights(serviceId, newWeights);
        }
    }

    /** Change the weights of the load balancer according to the given AdaptationOption.
     * Involves updating the knowledge accordingly and contacting the Config Manager and the Instances Manager actuators.
     *
     * @param changeLoadBalancerWeightsOption the AdaptationOption defining the new weights and
     *                                       the instances to remove the weights of (i.e., the instances that will be shut down)
     */
    private void handleChangeLBWeightsOption(ChangeLoadBalancerWeightsOption changeLoadBalancerWeightsOption) {
        String serviceId = changeLoadBalancerWeightsOption.getServiceId();
        Map<String, Double> newWeights = changeLoadBalancerWeightsOption.getNewWeights();
        changeLoadBalancerWeightsOption.getInstancesToShutdownIds().forEach(instanceToShutdownId -> {
            actuatorShutdownInstance(instanceToShutdownId);
            knowledgeClient.notifyShutdownInstance(new ShutdownInstanceRequest(serviceId, instanceToShutdownId));
        });
        actuatorUpdateLoadbalancerWeights(serviceId, newWeights, changeLoadBalancerWeightsOption.getInstancesToShutdownIds());
        knowledgeClient.setLoadBalancerWeights(serviceId, newWeights);
    }

    /** Change the implementation of a given service.
     *  Involves updating the knowledge accordingly, and contacting the Config Manager and the Instances Manager actuators.
     *
     * @param changeImplementationOption the AdaptationOption defining the new implementation of the service and
     *                                   the number of instances to start
     */
    private void handleChangeImplementationOption(ChangeImplementationOption changeImplementationOption) {
        String serviceId = changeImplementationOption.getServiceId();
        Service service = knowledgeClient.getService(serviceId);
        ServiceImplementation oldImplementation = service.getCurrentImplementation();

        // Start new instances of the new implementation
        StartNewInstancesResponse instancesResponse = instancesManagerClient.addInstances(new StartNewInstancesRequest(changeImplementationOption.getNewImplementationId(), changeImplementationOption.getNumberOfInstances()));
        if (instancesResponse.getDockerizedInstances().isEmpty())
            throw new RuntimeException("No instances were added");

        // Remove the old implementation instances and their weights
        List<String> oldInstancesIds = oldImplementation.getInstances().values().stream().collect(LinkedList::new, (list, instance) -> list.add(instance.getInstanceId()), List::addAll);
        oldInstancesIds.forEach(this::actuatorShutdownInstance);
        actuatorRemoveLoadBalancerWeights(serviceId, oldInstancesIds);

        // Update knowledge with the new instances
        List<String> newInstancesAddresses = instancesResponse.getDockerizedInstances().stream().collect(LinkedList::new, (list, instance) -> list.add(instance.getAddress()), List::addAll);
        knowledgeClient.notifyChangeOfImplementation(new ChangeOfImplementationRequest(serviceId, changeImplementationOption.getNewImplementationId(), newInstancesAddresses));
    }



    // Actuator methods

    /**
     * Contacts the instanceManager actuator to shut down the instance.
     *
     * @param instanceToRemoveId the id of the instance to shut down
     */
    private void actuatorShutdownInstance(String instanceToRemoveId) {
        String[] ipPort = instanceToRemoveId.split("@")[1].split(":");
        instancesManagerClient.removeInstance(new RemoveInstanceRequest(instanceToRemoveId.split("@")[0], ipPort[0], Integer.parseInt(ipPort[1])));
    }

    /**
     * Contacts the Config Manager actuator to change the weights of the load balancer of the specified service.
     *
     * @param serviceId the id of the service to change the weights of
     * @param weights the new weights of the load balancer for the active instances
     * @param instancesToShutdownIds the instances that will be shut down
     */
    private void actuatorUpdateLoadbalancerWeights(String serviceId, Map<String, Double> weights, List<String> instancesToShutdownIds) {
        List<PropertyToChange> propertyToChangeList = new LinkedList<>();
        weights.forEach((instanceId, weight) -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            propertyToChangeList.add(new PropertyToChange(null, propertyKey, weight.toString()));
        });
        instancesToShutdownIds.forEach(instanceId -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            propertyToChangeList.add(new PropertyToChange(null, propertyKey));
        });
        configManagerClient.changeProperty(new ChangePropertyRequest(propertyToChangeList));
    }

    /**
     * Contacts the Config Manager actuator to remove the weights of the load balancer of the specified service for a set of instances.
     *
     * @param serviceId the id of the service to change the weights of
     * @param instanceIds the instances to remove the weights of
     */
    private void actuatorRemoveLoadBalancerWeights(String serviceId, List<String> instanceIds) {
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
