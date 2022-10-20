package it.polimi.saefa.analyse.externalInterfaces;

import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.saefa.knowledge.rest.AddQoSValueRequest;
import it.polimi.saefa.knowledge.rest.UpdateServiceQosCollectionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {
    @PutMapping("/rest/activeModule")
    ResponseEntity<String> notifyModuleStart(@RequestParam Modules module);

    @PostMapping("/rest/metrics/addMetrics")
    void addMetrics(@RequestBody InstanceMetricsSnapshot metrics);

    @PostMapping("/rest/metrics/addMetricsList")
    void addMetrics(@RequestBody List<InstanceMetricsSnapshot> metrics);

    @GetMapping("/rest/metrics/{metricsId}")
    InstanceMetricsSnapshot getMetrics(@PathVariable long metricsId);

    @GetMapping("/rest/metrics/get")
    List<InstanceMetricsSnapshot> getMetrics(
            //@RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId,
            //@RequestParam(required = false, name = "at") String timestamp, // The timestamp MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after
    );

    @GetMapping("/rest/metrics/getLatest")
    List<InstanceMetricsSnapshot> getLatestMetrics(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId
    );

    @GetMapping("/rest/services")
    List<Service> getServices();

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();

    @GetMapping("/rest/metrics/getLatestNBefore")
    List<InstanceMetricsSnapshot> getLatestNMetricsBeforeDate(
            @RequestParam String instanceId,
            @RequestParam(name = "before") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam int n
    );

    @GetMapping("/rest/metrics/getLatestNAfter")
    List<InstanceMetricsSnapshot> getLatestNMetricsAfterDate(
            @RequestParam String instanceId,
            @RequestParam(name = "after") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam int n
    );

    @GetMapping("/rest/metrics/getLatestNOfCurrentInstance")
    List<InstanceMetricsSnapshot> getLatestNMetricsOfCurrentInstance(
            @RequestParam String serviceId,
            @RequestParam String instanceId,
            @RequestParam int n
    );

    @PostMapping("/rest/addNewQoSValue")
    ResponseEntity<String> addNewQoSValue(@RequestBody AddQoSValueRequest request);

    @PostMapping("/rest/proposeAdaptationOptions")
    ResponseEntity<String> proposeAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions);

    @PostMapping("/rest/updateServicesQoSCollection")
    ResponseEntity<String> updateServicesQoSCollection(@RequestBody Map<String, QoSCollection> serviceQoSMap);

    @PostMapping("/rest/updateInstancesQoSCollection")
    ResponseEntity<String> updateInstancesQoSCollection(@RequestBody Map<String, Map<String, QoSCollection>> instanceQoSMap);

    @PutMapping("/rest/activeModule")
    String setFailedModule(@RequestParam Modules module);

    @PostMapping("/rest/service/{serviceId}/invalidateQosHistory")
    ResponseEntity<String> invalidateQosHistory(@PathVariable String serviceId);

    @PostMapping("/rest/updateServiceQosCollection")
    void updateServiceQosCollection(@RequestBody UpdateServiceQosCollectionRequest request);
}