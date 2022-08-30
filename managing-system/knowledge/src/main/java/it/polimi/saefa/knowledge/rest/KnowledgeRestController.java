package it.polimi.saefa.knowledge.rest;


import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import it.polimi.saefa.knowledge.persistence.PersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path="/rest/")
public class KnowledgeRestController {
    @Autowired
    private PersistenceService persistenceService;

    @PostMapping("/")
    public void addMetric(@RequestBody InstanceMetrics metrics) {
        log.debug("Adding metric for {}@{} at {}", metrics.getServiceId(), metrics.getInstanceId(), metrics.getTimestamp());
        persistenceService.addMetrics(metrics);
    }

    @GetMapping("/{metricsId}")
    public InstanceMetrics getMetrics(@PathVariable long metricsId) {
        return persistenceService.getMetrics(metricsId);
    }

    // The timestamp MUST be in the format yyyy-MM-dd'T'HH:mm:ss
    @GetMapping("/get")
    public InstanceMetrics getMetrics(@RequestParam String instanceId, @RequestParam(name = "at") String timestamp) {
        return persistenceService.getMetrics(instanceId, timestamp);
    }

    @GetMapping("/getAll")
    public List<InstanceMetrics> getMetrics() {
        return persistenceService.getMetrics();
    }

    @GetMapping("/getAll/{instanceId}")
    public List<InstanceMetrics> getAllMetricsOfInstance(@PathVariable String instanceId) {
        return persistenceService.getMetrics(instanceId);
    }

    @GetMapping("/getAll/{serviceId}")
    public List<InstanceMetrics> getAllMetricsOfService(@PathVariable String serviceId) {
        return persistenceService.getMetrics(serviceId);
    }

    @GetMapping("/getRecent/instance/{instanceId}")
    public InstanceMetrics getRecentMetricsOfInstance(@PathVariable String instanceId) {
        return persistenceService.findLatestByInstanceId(instanceId);
    }

    @GetMapping("/getRecent/service/{serviceId}")
    public Collection<InstanceMetrics> getRecentMetricsOfService(@PathVariable String serviceId) {
        return persistenceService.findLatestByServiceId(serviceId);
    }

}
