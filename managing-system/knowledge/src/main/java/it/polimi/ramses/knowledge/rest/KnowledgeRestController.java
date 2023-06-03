package it.polimi.ramses.knowledge.rest;

import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.ramses.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.ramses.knowledge.domain.architecture.Instance;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.ramses.knowledge.domain.KnowledgeService;
import it.polimi.ramses.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.ramses.knowledge.rest.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class KnowledgeRestController {
    @Autowired
    private KnowledgeService knowledgeService;

    @GetMapping("/activeModule")
    public Modules getActiveModule() {
        return knowledgeService.getActiveModule();
    }

    @PutMapping("/activeModule")
    public ResponseEntity<String> notifyModuleStart(@RequestParam Modules module) {
        knowledgeService.setActiveModule(module);
        return ResponseEntity.ok().body("Module start correctly notified");
    }

    @GetMapping("/failedModule")
    public Modules getFailedModule() {
        return knowledgeService.getFailedModule();
    }

    @PutMapping("/failedModule")
    public String setFailedModule(@RequestParam Modules module) {
        log.error("Module {} failed", module);
        knowledgeService.setFailedModule(module);
        return "Failed module " + module + " correctly set";
    }

    @GetMapping("/services")
    public List<Service> getServices() {
        return knowledgeService.getServicesList();
    }

    @GetMapping("/servicesMap")
    public Map<String, Service> getServicesMap() {
        return knowledgeService.getServicesMap();
    }

    @GetMapping("/service/{serviceId}")
    public Service getService(@PathVariable String serviceId) {
        return knowledgeService.getService(serviceId);
    }

    @GetMapping("/service/{serviceId}/instance/{instanceId}")
    public Instance getInstance(@PathVariable String serviceId, @PathVariable String instanceId) {
        return knowledgeService.getService(serviceId).getInstance(instanceId);
    }

    @GetMapping("/service/{serviceId}/latestAdaptationDate")
    public Date getServiceLatestAdaptationDate(@PathVariable String serviceId) {
        return knowledgeService.getLatestAdaptationDateForService(serviceId);
    }

    @PostMapping("/service/update")
    public void updateService(@RequestBody Service service) {
        knowledgeService.updateService(service);
    }
    
    @PostMapping("/service/{serviceId}/updateBenchmarks")
    public String updateServiceBenchmarks(@PathVariable String serviceId, @RequestBody UpdateBenchmarkRequest request) {
        knowledgeService.updateBenchmark(serviceId, request.getServiceImplementationId(), request.getQos(), request.getNewValue());
        return "Benchmark correctly updated";
    }


    // Monitor-related functions
    @PostMapping("/metrics/addMetricsBuffer")
    public void addMetricsFromBuffer(@RequestBody Queue<List<InstanceMetricsSnapshot>> metricsSnapshotBuffer) {
        knowledgeService.addMetricsFromBuffer(metricsSnapshotBuffer);
    }

    // Analyse-related functions
    @GetMapping("/metrics/getLatestNOfCurrentInstance")
    public List<InstanceMetricsSnapshot> getLatestNMetricsOfCurrentInstance(@RequestParam String serviceId, @RequestParam String instanceId, @RequestParam int n) {
        return knowledgeService.getLatestNMetricsOfCurrentInstance(serviceId, instanceId, n);
    }

    @GetMapping("/proposedAdaptationOptions")
    public Map<String, List<AdaptationOption>> getProposedAdaptationOptions() {
        return knowledgeService.getProposedAdaptationOptions();
    }

    @PostMapping("/proposeAdaptationOptions")
    public ResponseEntity<String> proposeAdaptationOptions(@RequestBody Map<String, List<AdaptationOption>> adaptationOptions) {
        knowledgeService.proposeAdaptationOptions(adaptationOptions);
        return ResponseEntity.ok().body("Adaptation options correctly proposed");
    }

    @PostMapping("/updateServicesQoSCollection")
    public ResponseEntity<String> updateServicesQoSCollection(@RequestBody Map<String, QoSCollection> serviceQoSMap) {
        serviceQoSMap.forEach((serviceId, qosCollection) -> {
            knowledgeService.updateServiceQoSCollection(serviceId, qosCollection);
        });
        return ResponseEntity.ok().body("Service QoS correctly updated");
    }

    @PostMapping("/updateInstancesQoSCollection")
    public ResponseEntity<String> updateInstancesQoSCollection(@RequestBody Map<String, Map<String, QoSCollection>> instanceQoSMap) {
        instanceQoSMap.forEach((serviceId, instanceqosCollection) -> {
            instanceqosCollection.forEach((instanceId, qosCollection) -> {
                knowledgeService.updateInstanceQoSCollection(serviceId, instanceId, qosCollection);
            });
        });
        return ResponseEntity.ok().body("Instance QoS correctly updated");
    }

    @PostMapping("/updateServiceQosCollection")
    public void updateServiceQosCollection(@RequestBody UpdateServiceQosCollectionRequest request) {
        knowledgeService.updateServiceQosCollection(request.getServiceId(), request.getNewInstancesValues(), request.getNewServiceValues(), request.getNewInstancesCurrentValues(), request.getNewServiceCurrentValues());
    }

    // Plan-related functions
    @GetMapping("/chosenAdaptationOptions")
    public Map<String, List<AdaptationOption>> getChosenAdaptationOptions() {
        return knowledgeService.getChosenAdaptationOptions();
    }

    @PostMapping("/chooseAdaptationOptions")
    public ResponseEntity<String> chooseAdaptationOptions(@RequestBody Map<String, List<AdaptationOption>> adaptationOptions) {
        knowledgeService.chooseAdaptationOptions(adaptationOptions);
        return ResponseEntity.ok().body("Adaptation options correctly chosen");
    }

    // Execute-related functions
    @PostMapping("/changeConfiguration")
    public ResponseEntity<String> changeConfiguration(@RequestBody Map<String, ServiceConfiguration> request) {
        knowledgeService.changeServicesConfigurations(request);
        return ResponseEntity.ok("Configuration changed");
    }

    @PostMapping("/service/{serviceId}/setLoadBalancerWeights")
    public ResponseEntity<String> setLoadBalancerWeights(@PathVariable String serviceId, @RequestBody Map<String, Double> instanceWeights) {
        knowledgeService.setLoadBalancerWeights(serviceId, instanceWeights);
        return ResponseEntity.ok().body("Load balancer weights correctly set");
    }

    @PostMapping("/notifyShutdown")
    public ResponseEntity<String> notifyShutdownInstance(@RequestBody ShutdownInstanceRequest request) {
        knowledgeService.markInstanceAsShutdown(request.getServiceId(), request.getInstanceId());
        return ResponseEntity.ok("Shutdown of instance " + request.getInstanceId() + " notified");
    }

    @PostMapping("/notifyAddInstance")
    public ResponseEntity<String> notifyAddInstance(@RequestBody AddInstanceRequest request) {
        knowledgeService.addInstance(request.getServiceId(), request.getNewInstanceAddress());
        return ResponseEntity.ok("Add of instance " + request.getNewInstanceAddress() + " notified");
    }

    @PostMapping("/notifyChangeOfImplementation")
    public ResponseEntity<String> notifyChangeOfImplementation(@RequestBody ChangeOfImplementationRequest request) {
        knowledgeService.changeServiceImplementation(request.getServiceId(), request.getNewImplementationId(), request.getNewInstancesAddresses());
        return ResponseEntity.ok("Change of implementation to " + request.getNewImplementationId() + " notified");
    }

    @PostMapping("/service/{serviceId}/invalidateQosHistory")
    public ResponseEntity<String> invalidateQosHistory(@PathVariable String serviceId){
        knowledgeService.invalidateQosHistory(serviceId);
        return ResponseEntity.ok("QoS history invalidated");
    }


    @PutMapping("/service/{serviceId}/implementation/{implementationId}/preference")
    public ResponseEntity<String> updateImplementationPreference(@PathVariable String serviceId, @PathVariable String implementationId, @RequestParam(name = "value") double preference){
        knowledgeService.updateImplementationPreference(serviceId, implementationId, preference);
        return ResponseEntity.ok("Implementation preference updated");
    }

    @PutMapping("/service/{serviceId}/availabilityThreshold")
    public ResponseEntity<String> updateAvailabilityThreshold(@PathVariable String serviceId, @RequestParam(name = "value") double availabilityThreshold){
        knowledgeService.updateAvailabilityThreshold(serviceId, availabilityThreshold);
        return ResponseEntity.ok("Availability threshold updated");
    }

    @PutMapping("/service/{serviceId}/responseTimeThreshold")
    public ResponseEntity<String> updateResponseTimeThreshold(@PathVariable String serviceId, @RequestParam(name = "value") double responseTimeThreshold){
        knowledgeService.updateResponseTimeThreshold(serviceId, responseTimeThreshold);
        return ResponseEntity.ok("Response time threshold updated");
    }



    // Inspection endpoints
    @GetMapping("/chosenAdaptationOptionsHistory")
    public Map<String, List<AdaptationOption>> getChosenAdaptationOptionsHistory(@RequestParam int n) {
        Map<String, List<AdaptationOption>> history = new HashMap<>();
        knowledgeService.getServicesMap().keySet().forEach(serviceId -> {
            history.put(serviceId, knowledgeService.getChosenAdaptationOptionsHistory(serviceId, n));
        });
        return history;
    }

    @GetMapping("/metrics/getLatest")
    public List<InstanceMetricsSnapshot> getLatestMetrics(@RequestParam(required = false) String serviceId, @RequestParam(required = false) String instanceId) {
        if (serviceId == null && instanceId == null)
            throw new IllegalArgumentException("Invalid query arguments");
        if (instanceId != null)
            try {
                return List.of(knowledgeService.getLatestByInstanceId(instanceId));
            } catch (NullPointerException e) { return List.of(); }
        else
            return knowledgeService.getAllLatestByServiceId(serviceId);
    }


    @GetMapping("/metrics/get")
    public List<InstanceMetricsSnapshot> getMetrics(
            @RequestParam(required = false) String instanceId,
            @RequestParam(required = false, name = "after") String startDate, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
            @RequestParam(required = false, name = "before") String endDate // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
    ) {
        // before + after
        if (instanceId == null && startDate != null && endDate != null/* && serviceId == null*/)
            return knowledgeService.getAllMetricsBetween(startDate, endDate);

        // instanceId
        if (instanceId != null) {
            // + startDate + endDate
            if (startDate != null && endDate != null)
                return knowledgeService.getAllInstanceMetricsBetween(instanceId, startDate, endDate);
            // all
            if (startDate == null && endDate == null)
                return knowledgeService.getAllInstanceMetrics(instanceId);
        }
        throw new IllegalArgumentException("Invalid query arguments");
    }


    @GetMapping("/metrics/{metricsId}")
    public InstanceMetricsSnapshot getMetrics(@PathVariable long metricsId) {
        return knowledgeService.getMetrics(metricsId);
    }

    @GetMapping("/metrics/getLatestNBefore")
    public List<InstanceMetricsSnapshot> getLatestNMetricsBeforeDate(
            @RequestParam String instanceId,
            @RequestParam(name = "before") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
            @RequestParam int n
    ) {
        return knowledgeService.getNMetricsBefore(instanceId, timestamp, n);
    }

    @GetMapping("/metrics/getLatestNAfter")
    public List<InstanceMetricsSnapshot> getLatestNMetricsAfterDate(
            @RequestParam String instanceId,
            @RequestParam(name = "after") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
            @RequestParam int n
    ) {
        return knowledgeService.getNMetricsAfter(instanceId, timestamp, n);
    }
}