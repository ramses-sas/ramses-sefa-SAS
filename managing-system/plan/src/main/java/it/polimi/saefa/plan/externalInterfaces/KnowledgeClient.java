package it.polimi.saefa.plan.externalInterfaces;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Service;
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

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @PostMapping("/rest/addNewAdaptationParameterValue")
    ResponseEntity<String> addNewAdaptationParameterValue(@RequestBody AddAdaptationParameterValueRequest request);

    @GetMapping("/rest/proposedAdaptationOptions")
    Map<String, List<AdaptationOption>> proposedAdaptationOptions();

    @PostMapping("/rest/chooseAdaptationOptions")
    ResponseEntity<String> chooseAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions);

    @GetMapping("/rest/proposedAdaptationOptions")
    Map<String, List<AdaptationOption>> getProposedAdaptationOptions();
}