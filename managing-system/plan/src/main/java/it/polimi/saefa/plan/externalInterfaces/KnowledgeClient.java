package it.polimi.saefa.plan.externalInterfaces;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import it.polimi.saefa.knowledge.rest.AddAdaptationParameterValueRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @GetMapping("/rest/services")
    List<Service> getServices();

    @PostMapping("/rest/addNewAdaptationParameterValue")
    ResponseEntity<String> addNewAdaptationParameterValue(@RequestBody AddAdaptationParameterValueRequest request);

    @GetMapping("/rest/proposedAdaptationOptions")
    Map<String, List<AdaptationOption>> proposedAdaptationOptions();

    @PostMapping("/rest/chooseAdaptationOptions")
    ResponseEntity<String> chooseAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions);

    @GetMapping("/proposedAdaptationOptions")
    Map<String, List<AdaptationOption>> getProposedAdaptationOptions();
}