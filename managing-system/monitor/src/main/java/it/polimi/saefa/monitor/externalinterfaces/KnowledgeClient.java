package it.polimi.saefa.monitor.externalinterfaces;

import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@FeignClient(name = "KNOWLEDGE", url = "http://localhost:58005")
public interface KnowledgeClient {
    @GetMapping("/rest/getAll")
    List<InstanceMetrics> getMetrics();

    @GetMapping("/{metricsId}")
    InstanceMetrics getMetrics(@PathVariable long metricsId);

    // The timestamp MUST be in the format yyyy-MM-dd'T'HH:mm:ss
    @GetMapping("/get")
    public InstanceMetrics getMetrics(@RequestParam String instanceId, @RequestParam(name = "at") String timestamp);

    @PostMapping("/rest/get")
    void addMetrics(@RequestBody InstanceMetrics metrics);
}
