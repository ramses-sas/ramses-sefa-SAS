package it.polimi.saefa.execute.externalInterfaces;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {
    @GetMapping("/rest/services")
    List<Service> getServices();

    @GetMapping("/rest/chosenAdaptationOptions")
    List<AdaptationOption> getChosenAdaptationOptions();

}