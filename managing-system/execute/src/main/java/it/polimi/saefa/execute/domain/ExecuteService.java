package it.polimi.saefa.execute.domain;

import it.polimi.saefa.execute.externalInterfaces.*;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.options.AddInstances;
import it.polimi.saefa.knowledge.domain.adaptation.options.RemoveInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@org.springframework.stereotype.Service
public class ExecuteService {

    @Autowired
    private KnowledgeClient knowledgeClient;
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
            } else if (clazz.equals(RemoveInstance.class)) {
                handleRemoveInstance((RemoveInstance) (adaptationOption));
            } else {
                log.error("Unknown adaptation option type: " + adaptationOption.getClass());
            }
        });
        log.info("Execute step completed");
    }

    private void handleAddInstances(AddInstances addInstancesOption) {
        instancesManagerClient.addInstances(new AddInstancesRequest(addInstancesOption.getServiceImplementationId(), addInstancesOption.getNumberOfInstancesToAdd()));
    }

    private void handleRemoveInstance(RemoveInstance removeInstanceOption) {
        String[] ipPort = removeInstanceOption.getInstanceId().split("@")[0].split(":");
        instancesManagerClient.removeInstance(new RemoveInstanceRequest(removeInstanceOption.getServiceImplementationId(), ipPort[0], Integer.parseInt(ipPort[1])));
    }

    private void changeLBWeight() {
        //TODO
    }
}
