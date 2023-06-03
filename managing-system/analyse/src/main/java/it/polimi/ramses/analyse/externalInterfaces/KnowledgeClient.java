package it.polimi.ramses.analyse.externalInterfaces;

import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.ramses.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.ramses.knowledge.rest.api.UpdateServiceQosCollectionRequest;
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

    @GetMapping("/rest/metrics/getLatestNOfCurrentInstance")
    List<InstanceMetricsSnapshot> getLatestNMetricsOfCurrentInstance(
            @RequestParam String serviceId,
            @RequestParam String instanceId,
            @RequestParam int n
    );

    @PostMapping("/rest/proposeAdaptationOptions")
    ResponseEntity<String> proposeAdaptationOptions(@RequestBody Map<String, List<AdaptationOption>> adaptationOptions);

    @PutMapping("/rest/failedModule")
    String setFailedModule(@RequestParam Modules module);

    @PostMapping("/rest/updateServiceQosCollection")
    void updateServiceQosCollection(@RequestBody UpdateServiceQosCollectionRequest request);
}