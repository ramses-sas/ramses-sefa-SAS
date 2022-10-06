package it.polimi.saefa.execute.externalInterfaces;

import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @PostMapping("/notifyModuleStart")
    ResponseEntity<String> notifyModuleStart(@RequestBody Modules module);

    @GetMapping("/rest/services")
    List<Service> getServices();

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @GetMapping("/rest/chosenAdaptationOptions")
    List<AdaptationOption> getChosenAdaptationOptions();

    @PostMapping("/rest/setLoadBalancerWeights")
    ResponseEntity<String> setLoadBalancerWeights(@RequestBody Map<String, Map<String, Double>> servicesWeights);

    @PostMapping("/rest/service/update")
    void updateService(@RequestBody Service service);
}