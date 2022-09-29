package it.polimi.saefa.plan.domain;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.ChangeLoadBalancerWeights;
import it.polimi.saefa.plan.externalInterfaces.KnowledgeClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Slf4j
@org.springframework.stereotype.Service
public class PlanService {

    @Autowired
    private KnowledgeClient knowledgeClient;

    public void startPlan() {
        Map<String, List<AdaptationOption>> adaptationOptions = knowledgeClient.getProposedAdaptationOptions();

        for (String serviceId : adaptationOptions.keySet()) {
            List<AdaptationOption> options = adaptationOptions.get(serviceId);
            for (AdaptationOption option : options) {
                log.info("Adaptation option: {}", option.getDescription());
                if(option.getClass().equals(ChangeLoadBalancerWeights.class)){

                }



            }


        }
    }
}
