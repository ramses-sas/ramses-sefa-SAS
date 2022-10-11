package it.polimi.saefa.dashboard.externalinterfaces;

import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @GetMapping("/rest/metrics/getLatest")
    List<InstanceMetricsSnapshot> getLatestMetrics(@RequestParam(required = false) String serviceId, @RequestParam(required = false) String instanceId);

    @GetMapping("/rest/services")
    List<Service> getServices();

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @GetMapping("/rest/service/{serviceId}")
    Service getService(@PathVariable String serviceId);

    @GetMapping("/rest/service/{serviceId}/instance/{instanceId}")
    Instance getInstance(@PathVariable String serviceId, @PathVariable String instanceId);

    @GetMapping("/rest/service/{serviceId}/latestAdaptationDate")
    Date getServiceLatestAdaptationDate(@PathVariable String serviceId);


    // Adaptation status functions
    @GetMapping("/rest/activeModule")
    Modules getActiveModule();

    @GetMapping("/rest/proposedAdaptationOptions")
    Map<String, List<AdaptationOption>> getProposedAdaptationOptions();

    @GetMapping("/rest/chosenAdaptationOptions")
    Map<String, List<AdaptationOption>> getChosenAdaptationOptions();

}