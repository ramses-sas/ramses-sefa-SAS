package it.polimi.saefa.knowledge.domain;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.saefa.knowledge.domain.persistence.AdaptationChoicesRepository;
import it.polimi.saefa.knowledge.domain.persistence.ConfigurationRepository;
import it.polimi.saefa.knowledge.domain.persistence.MetricsRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@org.springframework.stereotype.Service
public class KnowledgeService {
    @Autowired
    private MetricsRepository metricsRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AdaptationChoicesRepository adaptationChoicesRepository;

    private final Map<String, Service> services = new ConcurrentHashMap<>();
    //va capito come vogliamo gestirlo, ovvero se
    // quando tutte le istanze di un servizio sono spente/crashate vogliamo che il servizio venga rimosso o meno
    // Scelta progettuale: una volta che un servizio è stato creato non viene mai rimosso, ma solo le sue istanze
    // (altrimenti è come il cambio di un functional requirement)

    private Set<Instance> previouslyActiveInstances = new HashSet<>();
    //private final Set<Instance> shutdownInstances = Collections.synchronizedSet(new HashSet<>());
    // <serviceId, AdaptationOptions proposed by the Analyse>
    @Getter @Setter
    private Map<String, List<AdaptationOption>> proposedAdaptationOptions = new HashMap<>();
    // <serviceId, AdaptationOption chosen by the Plan>
    @Getter @Setter
    private Map<String, AdaptationOption> chosenAdaptationOptions = new HashMap<>();

    @Getter
    private Modules activeModule = null;

    @Getter
    private Date lastAdaptationDate = new Date();


    public void setActiveModule(Modules activeModule) {
        this.activeModule = activeModule;
        if (activeModule == Modules.MONITOR) {
            // a new loop is started: reset the previous chosen options and the current proposed adaptation options
            if (!chosenAdaptationOptions.isEmpty())
                lastAdaptationDate = new Date();
            proposedAdaptationOptions = new HashMap<>();
            chosenAdaptationOptions = new HashMap<>();
        }
    }

    public void addService(Service service){
        services.put(service.getServiceId(), service);
    }

    public List<Service> getServicesList(){
        return services.values().stream().toList();
    }

    public Map<String,Service> getServicesMap() { return services; }

    public void breakpoint(){
        log.info("breakpoint");
    }

    public void addMetricsFromBuffer(Queue<List<InstanceMetricsSnapshot>> metricsBuffer) {
        try {
            log.info("Saving new set of metrics");
            for (List<InstanceMetricsSnapshot> metricsList : metricsBuffer) {
                Set<Instance> currentlyActiveInstances = new HashSet<>();
                Set<Instance> shutdownInstances = new HashSet<>();
                for (InstanceMetricsSnapshot metricsSnapshot : metricsList) {
                    Service service = services.get(metricsSnapshot.getServiceId()); //TODO l'executor deve notificare la knowledge quando un servizio cambia il microservizio che lo implementa
                    Instance instance = service.getInstance(metricsSnapshot.getInstanceId());
                    // If the instance has been shutdown, skip its metrics snapshot in the buffer. Next buffer won't contain its metrics snapshots.
                    if (instance.getCurrentStatus() != InstanceStatus.SHUTDOWN) {
                        if (!instance.getLatestInstanceMetricsSnapshot().equals(metricsSnapshot)) {
                            metricsRepository.save(metricsSnapshot);
                            instance.setLatestInstanceMetricsSnapshot(metricsSnapshot);
                            instance.setCurrentStatus(metricsSnapshot.getStatus());
                        } else
                            log.warn("Metrics Snapshot already saved: " + metricsSnapshot);
                        if (metricsSnapshot.isActive() || metricsSnapshot.isUnreachable())
                            currentlyActiveInstances.add(instance);
                    } else
                        shutdownInstances.add(instance);
                }
                // Failure detection of instances
                if (!previouslyActiveInstances.isEmpty()) {
                    Set<Instance> failedInstances = new HashSet<>(previouslyActiveInstances);
                    failedInstances.removeAll(currentlyActiveInstances);
                    failedInstances.removeAll(shutdownInstances);
                    failedInstances.forEach(instance -> {
                        instance.setCurrentStatus(InstanceStatus.FAILED);
                        InstanceMetricsSnapshot metrics = new InstanceMetricsSnapshot(instance.getServiceId(), instance.getInstanceId());
                        metrics.setStatus(InstanceStatus.FAILED);
                        metrics.applyTimestamp();
                        metricsRepository.save(metrics);
                    });
                }
                previouslyActiveInstances = new HashSet<>(currentlyActiveInstances);
            }
            // For each service, remove from the map of instances the instances that have been shutdown and their weights in the service configuration
            services.values().forEach(Service::removeShutdownInstances);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void notifyShutdownInstance(String serviceId, String instanceId) {
        Instance instance = services.get(serviceId).getInstance(instanceId);
        instance.setCurrentStatus(InstanceStatus.SHUTDOWN);
        InstanceMetricsSnapshot metrics = new InstanceMetricsSnapshot(instance.getServiceId(), instance.getInstanceId());
        metrics.setStatus(InstanceStatus.SHUTDOWN);
        metrics.applyTimestamp();
        metricsRepository.save(metrics);
    }

    public InstanceMetricsSnapshot getMetrics(long id) {
        return metricsRepository.findById(id).orElse(null);
    }

    public void changeServicesConfigurations(Map<String, ServiceConfiguration> newConfigurations){
        for (String serviceId : newConfigurations.keySet()){
            Service service = services.get(serviceId);
            service.setConfiguration(newConfigurations.get(serviceId));
            configurationRepository.save(newConfigurations.get(serviceId));
        }
    }



    public List<InstanceMetricsSnapshot> getLatestNMetricsOfCurrentInstance(String instanceId, int n) {
        return metricsRepository.findLatestOfCurrentInstanceOrderByTimestampDesc(instanceId, lastAdaptationDate, Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getAllInstanceMetricsBetween(String instanceId, String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampBetween(instanceId, startDate, endDate).stream().toList();
    }

    public InstanceMetricsSnapshot getLatestByInstanceId(String instanceId) {
        return metricsRepository.findLatestByInstanceId(instanceId).stream().findFirst().orElse(null);
    }



    public List<InstanceMetricsSnapshot> getAllLatestByServiceId(String serviceId) {
        return metricsRepository.findLatestByServiceId(serviceId).stream().toList();
    }

    public Service getService(String serviceId) {
        return services.get(serviceId);
    }

    public List<AdaptationOption> getAdaptationOptions(String serviceId, int n) {
        return adaptationChoicesRepository.findAllByServiceIdOrderByTimestampDesc(serviceId, Pageable.ofSize(n)).stream().toList();
    }

    public void proposeAdaptationOptions(Map<String, List<AdaptationOption>> proposedAdaptationOptions) {
        this.proposedAdaptationOptions = proposedAdaptationOptions;
    }

    // Called by the Plan module to choose the adaptation options
    public void chooseAdaptationOptions(List<AdaptationOption> options) {
        // add the options both to the repository and to the map
        options.forEach(option -> {
            option.applyTimestamp();
            adaptationChoicesRepository.save(option);
            chosenAdaptationOptions.put(option.getServiceId(), option);
        });
    }

    public void addNewInstanceAdaptationParameterValue(String serviceId, String instanceId, Class<? extends AdaptationParamSpecification> adaptationParameterClass, Double value) {
        services.get(serviceId).getInstance(instanceId).getAdaptationParamCollection().addNewAdaptationParamValue(adaptationParameterClass, value);
    }

    public void addNewServiceAdaptationParameterValue(String serviceId, Class<? extends AdaptationParamSpecification> adaptationParameterClass, Double value) {
        services.get(serviceId).getCurrentImplementation().getAdaptationParamCollection().addNewAdaptationParamValue(adaptationParameterClass, value);
    }

    public void setLoadBalancerWeights(String serviceId, Map<String, Double> weights) { // serviceId, Map<instanceId, weight>
        Service service = services.get(serviceId);
        service.getConfiguration().setLoadBalancerWeights(weights);
        configurationRepository.save(service.getConfiguration());
    }

    public void updateServiceAdaptationParamCollection(String serviceId, AdaptationParamCollection adaptationParamCollection) {
        services.get(serviceId).getCurrentImplementation().setAdaptationParamCollection(adaptationParamCollection);
    }

    public void updateInstanceParamAdaptationCollection(String serviceId, String instanceId, AdaptationParamCollection adaptationParamCollection) {
        services.get(serviceId).getInstance(instanceId).setAdaptationParamCollection(adaptationParamCollection);
    }

    public void updateService(Service service) {
        services.put(service.getServiceId(), service);
    }






    // Useful methods to investigate the metrics of the instances

    public List<InstanceMetricsSnapshot> getAllInstanceMetrics(String instanceId) {
        return metricsRepository.findAllByInstanceId(instanceId).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getAllMetricsBetween(String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByTimestampBetween(startDate, endDate).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getNMetricsBefore(String instanceId, String timestampStr, int n) {
        Date timestamp = Date.from(LocalDateTime.parse(timestampStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampBeforeOrderByTimestampDesc(instanceId, timestamp, Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetricsSnapshot> getNMetricsAfter(String instanceId, String timestampStr, int n) {
        Date timestamp = Date.from(LocalDateTime.parse(timestampStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampAfterOrderByTimestampDesc(instanceId, timestamp, Pageable.ofSize(n)).stream().toList();
    }

    public InstanceMetricsSnapshot getLatestActiveByInstanceId(String instanceId) {
        return metricsRepository.findLatestOnlineMeasurementByInstanceId(instanceId).stream().findFirst().orElse(null);
    }

}



/*
    public Service createNewInstances(String serviceId, String implementationId, List<String> instanceIds) {
        Service service = services.get(serviceId);
        int oldNumberOfInstances = service.getInstances().size();
        int newNumberOfInstances = oldNumberOfInstances + instanceIds.size();
        if(!service.getCurrentImplementationId().equals(implementationId)) {
            throw new RuntimeException("Implementation id mismatch");
        }

        for (String instanceId : instanceIds) {
                service.createInstance(instanceId);
        }

        for (Instance instance : service.getInstances()) {
            if (instanceIds.contains(instance.getInstanceId())) {
                service.setLoadBalancerWeight(instance, 1.0 / newNumberOfInstances);
            } else {
                service.setLoadBalancerWeight(instance, service.getLoadBalancerWeight(instance) * oldNumberOfInstances / newNumberOfInstances);
            }
        }
        return service;
    }

    Non più necessario per l'inserimento della seguente riga di codice al metodo addMetricsFromBuffer:
    shutdownInstances.removeIf(instance -> !currentlyActiveInstances.contains(instance)); //if the instance has been shut down and cannot be contacted from the monitor,
    public void notifyBootInstance(String serviceId, String instanceId) {
        shutdownInstances.remove(serviceId + "@" + instanceId);
    }

    public InstanceMetrics getMetrics(String serviceId, String instanceId, String timestamp) {
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp);
        Date date = Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
        return metricsRepository.findByServiceIdAndInstanceIdAndTimestamp(serviceId, instanceId, date);
    }

    public List<InstanceMetrics> getServiceMetrics(String serviceId) {
        return metricsRepository.findAllByServiceId(serviceId).stream().toList();
    }

    public List<InstanceMetrics> getMetrics() {
        List<InstanceMetrics> metrics = new LinkedList<>();
        metricsRepository.findAll().iterator().forEachRemaining(metrics::add);
        return metrics;
    }
 */