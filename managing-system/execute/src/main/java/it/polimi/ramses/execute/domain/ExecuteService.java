package it.polimi.ramses.execute.domain;

import it.polimi.ramses.execute.externalInterfaces.*;
import it.polimi.ramses.knowledge.domain.adaptation.options.*;
import it.polimi.ramses.configparser.CustomPropertiesWriter;
import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.domain.architecture.ServiceImplementation;
import it.polimi.ramses.knowledge.rest.api.AddInstanceRequest;
import it.polimi.ramses.knowledge.rest.api.ChangeOfImplementationRequest;
import it.polimi.ramses.knowledge.rest.api.ShutdownInstanceRequest;
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
        StartNewInstancesResponse instancesResponse = actuatorAddInstances(addInstanceOption.getServiceImplementationId(), 1);

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
            configManagerClient.changeLBWeights(new ChangeLBWeightsRequest(service.getServiceId(), newWeights, addInstanceOption.getInstancesToShutdownIds()));
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
            configManagerClient.changeLBWeights(new ChangeLBWeightsRequest(serviceId, newWeights, List.of(instanceToShutdownId)));
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
        configManagerClient.changeLBWeights(new ChangeLBWeightsRequest(serviceId, newWeights, changeLoadBalancerWeightsOption.getInstancesToShutdownIds()));
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
        configManagerClient.changeLBWeights(new ChangeLBWeightsRequest(serviceId, null, oldInstancesIds));

        // Update knowledge with the new instances
        List<String> newInstancesAddresses = instancesResponse.getDockerizedInstances().stream().collect(LinkedList::new, (list, instance) -> list.add(instance.getAddress()+":"+instance.getPort()), List::addAll);
        knowledgeClient.notifyChangeOfImplementation(new ChangeOfImplementationRequest(serviceId, changeImplementationOption.getNewImplementationId(), newInstancesAddresses));
    }

    private StartNewInstancesResponse actuatorAddInstances(String serviceImplementationId, int numberOfInstances) {
        return instancesManagerClient.addInstances(new StartNewInstancesRequest(serviceImplementationId, numberOfInstances));
    }

    /**
     * Contacts the instanceManager actuator to shut down the instance.
     *
     * @param instanceToRemoveId the id of the instance to shut down
     */
    private void actuatorShutdownInstance(String instanceToRemoveId) {
        String[] ipPort = instanceToRemoveId.split("@")[1].split(":");
        instancesManagerClient.removeInstance(new RemoveInstanceRequest(instanceToRemoveId.split("@")[0], ipPort[0], Integer.parseInt(ipPort[1])));
    }
}
