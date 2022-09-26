package it.polimi.saefa.plan.externalInterfaces;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import it.polimi.saefa.knowledge.rest.AddAdaptationParameterValueRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
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

    @GetMapping("/rest/metrics/getLatestNBefore")
    List<InstanceMetrics> getLatestNMetricsBeforeDate(
            @RequestParam String instanceId,
            @RequestParam(name = "before") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam int n
    );

    @GetMapping("/rest/metrics/getLatestNAfter")
    List<InstanceMetrics> getLatestNMetricsAfterDate(
            @RequestParam String instanceId,
            @RequestParam(name = "after") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss
            @RequestParam int n
    );

    @GetMapping("/rest/metrics/getLatestNOfCurrentInstance")
    List<InstanceMetrics> getLatestNMetricsOfCurrentInstance(
            @RequestParam String instanceId,
            @RequestParam int n
    );

    @PostMapping("/rest/addNewAdaptationParameterValue")
    ResponseEntity<String> addNewAdaptationParameterValue(@RequestBody AddAdaptationParameterValueRequest request);
}