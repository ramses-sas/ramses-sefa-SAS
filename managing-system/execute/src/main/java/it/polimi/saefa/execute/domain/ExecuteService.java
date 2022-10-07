package it.polimi.saefa.execute.domain;

import it.polimi.saefa.configparser.CustomPropertiesWriter;
import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.rest.CreateInstancesRequest;
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
        log.info("Starting Execute step");
        knowledgeClient.notifyModuleStart(Modules.EXECUTE);
        List<AdaptationOption> chosenAdaptationOptions = knowledgeClient.getChosenAdaptationOptions();
        chosenAdaptationOptions.forEach(adaptationOption -> {
            log.info("Executing adaptation option: " + adaptationOption.getDescription());
            Class<? extends AdaptationOption> clazz = adaptationOption.getClass();
            if (clazz.equals(AddInstances.class)) {
                handleAddInstances((AddInstances) (adaptationOption));
            } else if (clazz.equals(RemoveInstance.class)) {
                handleRemoveInstance((RemoveInstance) (adaptationOption));
            } else if (clazz.equals(ChangeLoadBalancerWeights.class)) {
                handleChangeLBWeight((ChangeLoadBalancerWeights) (adaptationOption));
            } else {
                log.error("Unknown adaptation option type: " + adaptationOption.getClass());
            }
        });
        log.info("Ending execute. Notifying Monitor module to continue the loop.");
        monitorClient.notifyFinishedIteration();
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

        int newNumberOfInstances = service.getInstances().size();
        int oldNumberOfInstances = newNumberOfInstances - instancesResponse.getDockerizedInstances().size();

        for (Instance instance : service.getInstances()) {
            if (newInstancesIds.contains(instance.getInstanceId())) {
                service.setLoadBalancerWeight(instance, 1.0 / newNumberOfInstances);
            } else {
                service.setLoadBalancerWeight(instance, service.getLoadBalancerWeight(instance) * oldNumberOfInstances / newNumberOfInstances);
            }
        }
        changeWeightsConfiguration(service.getServiceId(), service.getLoadBalancerWeights());
        knowledgeClient.updateService(service);
    }

    private void handleRemoveInstance(RemoveInstance removeInstancesOption) {
        Service service = knowledgeClient.getServicesMap().get(removeInstancesOption.getServiceId());
        Map<String, Instance> instancesMap = service.getInstancesMap();
        Instance instanceToRemove = instancesMap.get(removeInstancesOption.getInstanceId());
        Double instanceWeight = service.getLoadBalancerWeight(instanceToRemove);

        for(Instance instance : instancesMap.values()) {
            double weight = service.getLoadBalancerWeight(instance);
            if(!instance.equals(instanceToRemove)) {
                weight += instanceWeight / (instancesMap.size() - 1);
            }else {
                weight = 0.0;
            }
            service.setLoadBalancerWeight(instance, weight);
        }
        String[] ipPort = instanceToRemove.getAddress().split(":");
        instancesManagerClient.removeInstance(new RemoveInstanceRequest(removeInstancesOption.getServiceImplementationId(), ipPort[0], Integer.parseInt(ipPort[1])));
        changeWeightsConfiguration(service.getServiceId(), service.getLoadBalancerWeights());
        knowledgeClient.updateService(service);//todo magari alleggerire e magari mandare solo config
        knowledgeClient.notifyShutdownInstance(Map.of("serviceId", instanceToRemove.getServiceId(), "instanceId", instanceToRemove.getInstanceId()));
    }

    private void handleChangeLBWeight(ChangeLoadBalancerWeights changeLoadBalancerWeightsOption) {
        String serviceId = changeLoadBalancerWeightsOption.getServiceId();
        changeWeightsConfiguration(serviceId, changeLoadBalancerWeightsOption.getNewWeights());
        Map<String, Map<String, Double>> servicesWeights = new HashMap<>();
        servicesWeights.put(serviceId, changeLoadBalancerWeightsOption.getNewWeights());
        knowledgeClient.setLoadBalancerWeights(servicesWeights);
    }

    private void changeWeightsConfiguration(String serviceId, Map<String, Double> weights){
        List<PropertyToChange> propertyToChangeList = new LinkedList<>();
        weights.forEach((instanceId, weight) -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            propertyToChangeList.add(new PropertyToChange(null, propertyKey, weight.toString()));
        });
        configManagerClient.changeProperty(new ChangePropertyRequest(propertyToChangeList));
    }
}
