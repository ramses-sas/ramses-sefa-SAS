package it.polimi.ramses.plan.externalInterfaces;

import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @PutMapping("/rest/activeModule")
    ResponseEntity<String> notifyModuleStart(@RequestParam Modules module);

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @PostMapping("/rest/chooseAdaptationOptions")
    ResponseEntity<String> chooseAdaptationOptions(@RequestBody Map<String, List<AdaptationOption>> adaptationOptions);

    @GetMapping("/rest/proposedAdaptationOptions")
    Map<String, List<AdaptationOption>> getProposedAdaptationOptions();

    @PutMapping("/rest/failedModule")
    String setFailedModule(@RequestParam Modules module);

    @PostMapping("/rest/service/{serviceId}/invalidateQosHistory")
    ResponseEntity<String> invalidateQosHistory(@PathVariable String serviceId);
}