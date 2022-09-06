package it.polimi.saefa.knowledge.rest;


import it.polimi.saefa.knowledge.persistence.domain.Instance;
import it.polimi.saefa.knowledge.persistence.domain.InstanceMetrics;
import it.polimi.saefa.knowledge.persistence.KnowledgeService;
import it.polimi.saefa.knowledge.persistence.domain.ServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class KnowledgeRestController {

    @Autowired
    private KnowledgeService knowledgeService;

    /*
    @PostMapping("/addMetrics")
    public void addMetrics(@RequestBody InstanceMetrics metrics) {
        log.debug("Adding metric for {}@{} at {}", metrics.getServiceId(), metrics.getInstanceId(), metrics.getTimestamp());
        persistenceService.addMetrics(metrics);
    }*/

    @PostMapping("/addMetricsList")
    public void addMetrics(@RequestBody List<InstanceMetrics> metrics) {
        knowledgeService.addMetrics(metrics);
    }

    @GetMapping("/{metricsId}")
    public InstanceMetrics getMetrics(@PathVariable long metricsId) {
        return knowledgeService.getMetrics(metricsId);
    }

    @GetMapping("/get")
    public List<InstanceMetrics> getMetrics(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId,
            //@RequestParam(required = false, name = "at") String timestamp,
            @RequestParam(required = false, name = "after") String startDate, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam(required = false, name = "before") String endDate // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss
    ) {
        // before + after
        if (instanceId == null && startDate != null && endDate != null && serviceId == null)
            return knowledgeService.getAllMetricsBetween(startDate, endDate);

        // serviceId + instanceId
        if (serviceId != null && instanceId != null) {
            // + startDate + endDate
            if (startDate != null && endDate != null)
                return knowledgeService.getAllInstanceMetricsBetween(serviceId, instanceId, startDate, endDate);

            // all
            if (startDate == null && endDate == null)
                return knowledgeService.getAllInstanceMetrics(serviceId, instanceId);

            /* + timestamp
            if (timestamp != null && startDate == null && endDate == null)
                try {
                    return List.of(persistenceService.getMetrics(serviceId, instanceId, timestamp));
                } catch (NullPointerException e) { return List.of(); }
             */
        }
        throw new IllegalArgumentException("Invalid query arguments");
        //TODO se da nessuna altra parte lanciamo eccezioni (e quindi non serve un handler),
        // modificare il tipo di ritorno della funzione in "requestbody"
    }


    @GetMapping("/getLatest")
    public List<InstanceMetrics> getLatestMetrics(
            @RequestParam String serviceId,
            @RequestParam(required = false) String instanceId
    ) {
        if (instanceId != null)
            try {
                return List.of(knowledgeService.getLatestByInstanceId(serviceId, instanceId));
            } catch (NullPointerException e) { return List.of(); }
        else
            return knowledgeService.getAllLatestByServiceId(serviceId);
    }

    @PostMapping("/notifyShutdown")//todo per le rest vanno messi nello url gli ID anche se è una post? Poi avrebbe body vuoto, ma non voglio renderla get
    public ResponseEntity<String> notifyShutdownInstance(@RequestBody Instance shutDownInstance) {
        knowledgeService.notifyShutdownInstance(shutDownInstance);
        return ResponseEntity.ok("Shutdown of instance" + shutDownInstance.getInstanceId() + " notified");
    }

    @PostMapping("/changeConfiguration")
    public ResponseEntity<String> changeConfiguration(@RequestBody Map<String, ServiceConfiguration> request) {
        knowledgeService.changeServicesConfigurations(request);
        return ResponseEntity.ok("Configuration changed");
    }

    @GetMapping("/")
    public String hello() {
        return "Hello from Knowledge Service";
    }
}














/*
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
        return persistenceService.getLatestByInstanceId(instanceId);
    }

    @GetMapping("/getRecent/service/{serviceId}")
    public Collection<InstanceMetrics> getRecentMetricsOfService(@PathVariable String serviceId) {
        return persistenceService.getAllLatestByServiceId(serviceId);
    }

     */