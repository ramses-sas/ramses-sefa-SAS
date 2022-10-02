package it.polimi.saefa.knowledge.rest;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import it.polimi.saefa.knowledge.domain.KnowledgeService;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class KnowledgeRestController {

    @Autowired
    private KnowledgeService knowledgeService;

    @PostMapping("/metrics/addMetricsList")
    public void addMetrics(@RequestBody List<InstanceMetrics> metrics) {
        knowledgeService.addMetrics(metrics);
    }

    @GetMapping("/metrics/{metricsId}")
    public InstanceMetrics getMetrics(@PathVariable long metricsId) {
        return knowledgeService.getMetrics(metricsId);
    }

    @GetMapping("/metrics/getLatestNBefore")
    public List<InstanceMetrics> getLatestNMetricsBeforeDate(
            @RequestParam String instanceId,
            @RequestParam(name = "before") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
            @RequestParam int n
    ) {
        return knowledgeService.getNMetricsBefore(instanceId, timestamp, n);
    }

    @GetMapping("/metrics/getLatestNAfter")
    public List<InstanceMetrics> getLatestNMetricsAfterDate(
            @RequestParam String instanceId,
            @RequestParam(name = "after") String timestamp, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
            @RequestParam int n
    ) {
        return knowledgeService.getNMetricsAfter(instanceId, timestamp, n);
    }

    @GetMapping("/metrics/getLatestNOfCurrentInstance")
    public List<InstanceMetrics> getLatestNMetricsOfCurrentInstance(
            @RequestParam String instanceId,
            @RequestParam int n
    ) {
        return knowledgeService.getLatestNMetricsOfCurrentInstance(instanceId, n);
    }

    @GetMapping("/metrics/get")
    public List<InstanceMetrics> getMetrics(
            //@RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId,
            //@RequestParam(required = false, name = "at") String timestamp,
            @RequestParam(required = false, name = "after") String startDate, // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
            @RequestParam(required = false, name = "before") String endDate // The date MUST be in the format yyyy-MM-dd'T'HH:mm:ss (without the ' around the T)
    ) {
        // before + after
        if (instanceId == null && startDate != null && endDate != null/* && serviceId == null*/)
            return knowledgeService.getAllMetricsBetween(startDate, endDate);

        // instanceIdList
        if (/*serviceId != null && */instanceId != null) {
            // + startDate + endDate
            if (startDate != null && endDate != null)
                return knowledgeService.getAllInstanceMetricsBetween(instanceId, startDate, endDate);

            // all
            if (startDate == null && endDate == null)
                return knowledgeService.getAllInstanceMetrics(instanceId);

            /* + timestamp
            if (timestamp != null && startDate == null && endDate == null)
                try {
                    return List.of(persistenceService.getMetrics(serviceId, instanceIdList, timestamp));
                } catch (NullPointerException e) { return List.of(); }
             */
        }
        throw new IllegalArgumentException("Invalid query arguments");
        //TODO se da nessuna altra parte lanciamo eccezioni (e quindi non serve un handler),
        // modificare il tipo di ritorno della funzione in "requestbody"
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

    @GetMapping("/metrics/getLatest")
    public List<InstanceMetrics> getLatestMetrics(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String instanceId
    ) {
        if (serviceId == null && instanceId == null)
            throw new IllegalArgumentException("Invalid query arguments");
        if (instanceId != null)
            try {
                return List.of(knowledgeService.getLatestByInstanceId(instanceId));
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

    @PostMapping("/addNewAdaptationParameterValue")
    public ResponseEntity<String> addNewAdaptationParameterValue(@RequestBody AddAdaptationParameterValueRequest request){
        if(request.getInstanceId() == null){
            knowledgeService.addNewServiceAdaptationParameterValue(request.getServiceId(), request.getAdaptationParameterClass(), request.getValue());
        } else if (request.getInstanceId() != null){
            knowledgeService.addNewInstanceAdaptationParameterValue(request.getServiceId(), request.getInstanceId(), request.getAdaptationParameterClass(), request.getValue());

        }
        return ResponseEntity.ok().body("Adaptation parameter value added");
    }

    @GetMapping("/proposedAdaptationOptions")
    public Map<String, List<AdaptationOption>> getProposedAdaptationOptions() {
        return knowledgeService.getProposedAdaptationOptions();
    }

    @PostMapping("/proposeAdaptationOptions")
    public ResponseEntity<String> proposeAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions) {
        Map<String, List<AdaptationOption>> servicesAdaptOptions = new HashMap<>();
        adaptationOptions.forEach(adaptationOption -> {
            if (servicesAdaptOptions.containsKey(adaptationOption.getServiceId())){
                servicesAdaptOptions.get(adaptationOption.getServiceId()).add(adaptationOption);
            } else {
                List<AdaptationOption> adaptationOptionsList = new LinkedList<>();
                adaptationOptionsList.add(adaptationOption);
                servicesAdaptOptions.put(adaptationOption.getServiceId(), adaptationOptionsList);
            }
        });
        knowledgeService.setProposedAdaptationOptions(servicesAdaptOptions);
        return ResponseEntity.ok().body("Adaptation options correctly proposed");
    }

    @GetMapping("/chosenAdaptationOptions")
    public List<AdaptationOption> getChosenAdaptationOptions() {
        return knowledgeService.getChosenAdaptationOptions().values().stream().toList();
    }

    @PostMapping("/chooseAdaptationOptions")
    public ResponseEntity<String> chooseAdaptationOptions(@RequestBody List<AdaptationOption> adaptationOptions) {
        knowledgeService.chooseAdaptationOptions(adaptationOptions);
        return ResponseEntity.ok().body("Adaptation options correctly chosen");
    }

    // TODO remove after test
    @GetMapping("/")
    public String debug() {
        knowledgeService.breakpoint();
        return "Hello from Knowledge Service";
    }
}














/*
    @GetMapping("/getAll")
    public List<InstanceMetrics> getMetrics() {
        return persistenceService.getMetrics();
    }

    @GetMapping("/getAll/{instanceIdList}")
    public List<InstanceMetrics> getAllMetricsOfInstance(@PathVariable String instanceIdList) {
        return persistenceService.getMetrics(instanceIdList);
    }

    @GetMapping("/getAll/{serviceId}")
    public List<InstanceMetrics> getAllMetricsOfService(@PathVariable String serviceId) {
        return persistenceService.getMetrics(serviceId);
    }

    @GetMapping("/getRecent/instance/{instanceIdList}")
    public InstanceMetrics getRecentMetricsOfInstance(@PathVariable String instanceIdList) {
        return persistenceService.getLatestByInstanceId(instanceIdList);
    }

    @GetMapping("/getRecent/service/{serviceId}")
    public Collection<InstanceMetrics> getRecentMetricsOfService(@PathVariable String serviceId) {
        return persistenceService.getAllLatestByServiceId(serviceId);
    }

     */