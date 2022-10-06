package it.polimi.saefa.analyse.externalInterfaces;

import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import it.polimi.saefa.knowledge.rest.AddAdaptationParameterValueRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@FeignClient(name = "KNOWLEDGE", url = "${KNOWLEDGE_URL}")
public interface KnowledgeClient {
    @PostMapping("/notifyModuleStart")
    ResponseEntity<String> notifyModuleStart(@RequestBody Modules module);

    @PostMapping("/rest/metrics/addMetrics")
    void addMetrics(@RequestBody InstanceMetrics metrics);

    @PostMapping("/rest/metrics/addMetricsList")
    void addMetrics(@RequestBody List<InstanceMetrics> metrics);

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

    @PostMapping("/rest/proposeAdaptationOptions")
    ResponseEntity<String> proposeAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions);

    @PostMapping("/rest/updateServicesAdaptationParamCollection")
    ResponseEntity<String> updateServicesAdaptationParamCollection(@RequestBody Map<String, AdaptationParamCollection> serviceAdaptationParameters);

    @PostMapping("/rest/updateInstancesAdaptationParamCollection")
    ResponseEntity<String> updateInstancesAdaptationParamCollection(@RequestBody Map<String, Map<String, AdaptationParamCollection>> instanceAdaptationParameters);

    // TODO remove after test
    @PostMapping("/rest/chooseAdaptationOptions")
    ResponseEntity<String> chooseAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions);



}