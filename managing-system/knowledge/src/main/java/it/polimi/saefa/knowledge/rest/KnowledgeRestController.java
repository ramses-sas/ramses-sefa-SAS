package it.polimi.saefa.knowledge.rest;


import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import it.polimi.saefa.knowledge.persistence.PersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/")
    public InstanceMetrics getMetrics(Date timestamp, String instanceId) {
        return persistenceService.getMetrics(instanceId, timestamp);
    }

    @GetMapping("/getAll")
    public List<InstanceMetrics> getMetrics() {
        return persistenceService.getMetrics();
    }
}
