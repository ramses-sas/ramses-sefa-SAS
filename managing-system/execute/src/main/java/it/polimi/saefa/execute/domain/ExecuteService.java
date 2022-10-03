package it.polimi.saefa.execute.domain;

import it.polimi.saefa.configparser.CustomPropertiesWriter;
import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.adaptation.options.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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
        List<AdaptationOption> chosenAdaptationOptions = knowledgeClient.getChosenAdaptationOptions();
        chosenAdaptationOptions.forEach(adaptationOption -> {
            log.info("Executing adaptation option: " + adaptationOption.getDescription());
            Class<? extends AdaptationOption> clazz = adaptationOption.getClass();
            if (clazz.equals(AddInstances.class)) {
                handleAddInstances((AddInstances) (adaptationOption));
            } else if (clazz.equals(RemoveInstances.class)) {
                handleRemoveInstance((RemoveInstances) (adaptationOption));
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

    private void handleRemoveInstance(RemoveInstances removeInstancesOption) {
        for (String instanceId : removeInstancesOption.getInstanceIdList()) {
            String[] ipPort = instanceId.split("@")[0].split(":");
            instancesManagerClient.removeInstance(new RemoveInstanceRequest(removeInstancesOption.getServiceImplementationId(), ipPort[0], Integer.parseInt(ipPort[1])));
        }
    }

    private void handleChangeLBWeight(ChangeLoadBalancerWeights changeLoadBalancerWeightsOption) {
        String serviceId = changeLoadBalancerWeightsOption.getServiceId();
        changeLoadBalancerWeightsOption.getNewWeights().forEach((instanceId, weight) -> {
            String propertyKey = CustomPropertiesWriter.buildLoadBalancerInstanceWeightPropertyKey(serviceId, instanceId.split("@")[1]);
            configManagerClient.addOrUpdateProperty(new AddOrUpdatePropertyRequest(serviceId, propertyKey, weight.toString()));
        });
    }
}
