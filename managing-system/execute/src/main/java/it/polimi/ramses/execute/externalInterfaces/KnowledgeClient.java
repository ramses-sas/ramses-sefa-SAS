package it.polimi.ramses.execute.externalInterfaces;

import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.rest.api.AddInstanceRequest;
import it.polimi.ramses.knowledge.rest.api.ChangeOfImplementationRequest;
import it.polimi.ramses.knowledge.rest.api.ShutdownInstanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @PutMapping("/rest/activeModule")
    ResponseEntity<String> notifyModuleStart(@RequestParam Modules module);

    @GetMapping("/rest/service/{serviceId}")
    Service getService(@PathVariable String serviceId);

    @GetMapping("/rest/chosenAdaptationOptions")
    Map<String, List<AdaptationOption>> getChosenAdaptationOptions();

    @PostMapping("/rest/service/{serviceId}/setLoadBalancerWeights")
    ResponseEntity<String> setLoadBalancerWeights(@PathVariable String serviceId, @RequestBody Map<String, Double> instanceWeights);

    @PostMapping("/rest/notifyShutdown")
    ResponseEntity<String> notifyShutdownInstance(@RequestBody ShutdownInstanceRequest request);

    @PostMapping("/rest/notifyAddInstance")
    ResponseEntity<String> notifyAddInstance(@RequestBody AddInstanceRequest request);

    @PostMapping("/rest/notifyChangeOfImplementation")
    ResponseEntity<String> notifyChangeOfImplementation(@RequestBody ChangeOfImplementationRequest request);

    @PutMapping("/rest/failedModule")
    String setFailedModule(@RequestParam Modules module);
}