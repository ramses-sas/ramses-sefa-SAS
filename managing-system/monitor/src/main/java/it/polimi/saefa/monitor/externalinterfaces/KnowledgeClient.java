package it.polimi.saefa.monitor.externalinterfaces;

import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "KNOWLEDGE", url = "http://localhost:58005")
public interface KnowledgeClient {
    @PostMapping("/rest/addMetrics")
    void addMetrics(@RequestBody InstanceMetrics metrics);

    @PostMapping("/rest/addMetricsList")
    public void addMetrics(@RequestBody List<InstanceMetrics> metrics);

    @GetMapping("/rest/{metricsId}")
    InstanceMetrics getMetrics(@PathVariable long metricsId);

    @GetMapping("/rest/get")
    List<InstanceMetrics> getMetrics(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId,
            //@RequestParam(required = false, name = "at") String timestamp, // The timestamp MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after
    );

    @GetMapping("/rest/getLatest")
    List<InstanceMetrics> getLatestMetrics(
            @RequestParam String serviceId,
            @RequestParam(required = false) String instanceId
    );

}