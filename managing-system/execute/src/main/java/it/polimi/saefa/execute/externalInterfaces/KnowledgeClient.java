package it.polimi.saefa.execute.externalInterfaces;

import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.rest.AddInstanceRequest;
import it.polimi.saefa.knowledge.rest.ChangeOfImplementationRequest;
import it.polimi.saefa.knowledge.rest.ShutdownInstanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @PostMapping("/rest/notifyModuleStart")
    ResponseEntity<String> notifyModuleStart(@RequestBody Modules module);

    @GetMapping("/rest/services")
    List<Service> getServices();

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @GetMapping("/service/{serviceId}")
    Service getService(@PathVariable String serviceId);

    @GetMapping("/rest/chosenAdaptationOptions")
    Map<String, List<AdaptationOption>> getChosenAdaptationOptions();

    @PostMapping("/rest/service/{serviceId}/setLoadBalancerWeights")
    ResponseEntity<String> setLoadBalancerWeights(@PathVariable String serviceId, @RequestBody Map<String, Double> instanceWeights);

    @PostMapping("/rest/service/update")
    void updateService(@RequestBody Service service);

    @PostMapping("/rest/notifyShutdown")
    ResponseEntity<String> notifyShutdownInstance(@RequestBody ShutdownInstanceRequest request);

    @PostMapping("/rest/notifyAddInstance")
    ResponseEntity<String> notifyAddInstance(@RequestBody AddInstanceRequest request);

    @PostMapping("/rest/notifyChangeOfImplementation")
    ResponseEntity<String> notifyChangeOfImplementation(@RequestBody ChangeOfImplementationRequest request);
}