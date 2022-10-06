package it.polimi.saefa.execute.domain;

import it.polimi.saefa.configparser.CustomPropertiesWriter;
import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        instancesManagerClient.addInstances(new AddInstancesRequest(addInstancesOption.getServiceImplementationId(), addInstancesOption.getNumberOfInstancesToAdd()));
    }

    private void handleRemoveInstance(RemoveInstance removeInstancesOption) {
        String serviceId = removeInstancesOption.getServiceId();
        Service service = knowledgeClient.getServicesMap().get(serviceId);
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
        knowledgeClient.updateService(service);//todo magari alleggerire e magari mandare solo config
        knowledgeClient.notifyShutdownInstance(instanceToRemove);
    }

    private void handleChangeLBWeight(ChangeLoadBalancerWeights changeLoadBalancerWeightsOption) {
        String serviceId = changeLoadBalancerWeightsOption.getServiceId();
        List<PropertyToChange> propertyToChangeList = new LinkedList<>();
        changeLoadBalancerWeightsOption.getNewWeights().forEach((instanceId, weight) -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            propertyToChangeList.add(new PropertyToChange(null, propertyKey, weight.toString()));
        });
        configManagerClient.changeProperty(new ChangePropertyRequest(propertyToChangeList));
        Map<String, Map<String, Double>> servicesWeights = new HashMap<>();
        servicesWeights.put(serviceId, changeLoadBalancerWeightsOption.getNewWeights());
        knowledgeClient.setLoadBalancerWeights(servicesWeights);
    }
}
