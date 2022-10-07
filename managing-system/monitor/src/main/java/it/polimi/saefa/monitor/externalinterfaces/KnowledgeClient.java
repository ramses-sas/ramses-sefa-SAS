package it.polimi.saefa.monitor.externalinterfaces;

import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Queue;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {

    @PostMapping("/rest/notifyModuleStart")
    ResponseEntity<String> notifyModuleStart(@RequestBody Modules module);

    @PostMapping("/rest/metrics/addMetricsBuffer")
    void addMetricsFromBuffer(@RequestBody Queue<List<InstanceMetrics>> metricsSnapshotBuffer);

    @GetMapping("/rest/metrics/{metricsId}")
    InstanceMetrics getMetrics(@PathVariable long metricsId);

    @GetMapping("/rest/metrics/get")
    List<InstanceMetrics> getMetrics(
            //@RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId,
            //@RequestParam(required = false, name = "at") String timestamp, // The timestamp MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after
    );

    @GetMapping("/rest/metrics/getLatest")
    List<InstanceMetrics> getLatestMetrics(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId
    );

    @GetMapping("/rest/services")
    List<Service> getServices();

    @GetMapping("/rest/servicesMap")
    Map<String, Service> getServicesMap();
}