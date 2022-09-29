package it.polimi.saefa.execute.domain;

import it.polimi.saefa.execute.externalInterfaces.ConfigManagerClient;
import it.polimi.saefa.execute.externalInterfaces.InstancesManagerClient;
import it.polimi.saefa.execute.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AdaptationOption;
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
        });
        log.info("Execute step completed");
    }
}
