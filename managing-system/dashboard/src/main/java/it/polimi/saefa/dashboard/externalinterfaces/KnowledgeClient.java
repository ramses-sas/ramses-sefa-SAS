package it.polimi.saefa.dashboard.externalinterfaces;

import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {
    @PostMapping("/rest/metrics/addMetrics")
    void addMetrics(@RequestBody InstanceMetricsSnapshot metrics);

    @PostMapping("/rest/metrics/addMetricsList")
    public void addMetrics(@RequestBody List<InstanceMetricsSnapshot> metrics);

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

    @GetMapping("/rest/service/{serviceId}")
    Service getService(@PathVariable String serviceId);

}