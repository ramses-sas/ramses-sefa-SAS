package it.polimi.saefa.dashboard.externalinterfaces;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// TODO: add the correct url from application.properties
@FeignClient(name = "KNOWLEDGE", url = "http://localhost:58005")
public interface KnowledgeClient {
    @PostMapping("/rest/metrics/addMetrics")
    void addMetrics(@RequestBody InstanceMetrics metrics);

    @PostMapping("/rest/metrics/addMetricsList")
    public void addMetrics(@RequestBody List<InstanceMetrics> metrics);

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

    @GetMapping("/rest/service/{serviceId}")
    Service getService(@PathVariable String serviceId);

}