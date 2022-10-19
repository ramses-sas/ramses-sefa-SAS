package it.polimi.saefa.knowledge.domain;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSCollection;
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
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
public class KnowledgeService {
    @Autowired
    private MetricsRepository metricsRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AdaptationChoicesRepository adaptationChoicesRepository;

    @Getter
    private final Map<String, Service> servicesMap = new ConcurrentHashMap<>();

    private Set<Instance> previouslyActiveInstances = new HashSet<>();

    // <serviceId, AdaptationOptions proposed by the Analyse>
    @Getter @Setter
    private Map<String, List<AdaptationOption>> proposedAdaptationOptions = new HashMap<>();

    // <serviceId, AdaptationOptions chosen by the Plan (in this implementation, the Plan chooses ONE option per service)>
    @Getter @Setter
    private Map<String, List<AdaptationOption>> chosenAdaptationOptions = new HashMap<>();

    @Getter
    private Modules activeModule = null;

    @Getter @Setter
    private Modules failedModule = null;


    public void setActiveModule(Modules activeModule) {
        this.activeModule = activeModule;
        if (activeModule == Modules.MONITOR) {
            // a new loop is started: reset the previous chosen options and the current proposed adaptation options
            for (String serviceId : chosenAdaptationOptions.keySet()) {
                servicesMap.get(serviceId).setLatestAdaptationDate(new Date());
            }
            proposedAdaptationOptions = new HashMap<>();
            chosenAdaptationOptions = new HashMap<>();
        }
    }

    public Date getLatestAdaptationDateForService(String serviceId) {
        return servicesMap.get(serviceId).getLatestAdaptationDate();
    }

    // Called by the KnowledgeInit
    public void addService(Service service) {
        servicesMap.put(service.getServiceId(), service);
    }
    

    public List<Service> getServicesList(){
        return servicesMap.values().stream().toList();
    }

    public void breakpoint(){
        log.info("breakpoint");
    }

    public void addMetricsFromBuffer(Queue<List<InstanceMetricsSnapshot>> metricsBuffer) {
        try {
            Set<Instance> shutdownInstancesStillMonitored = new HashSet<>();
            log.info("Saving new set of metrics");
            for (List<InstanceMetricsSnapshot> metricsList : metricsBuffer) {
                Set<Instance> currentlyActiveInstances = new HashSet<>();
                for (InstanceMetricsSnapshot metricsSnapshot : metricsList) {
                    Service service = servicesMap.get(metricsSnapshot.getServiceId());
                    if (!Objects.equals(metricsSnapshot.getServiceImplementationId(), service.getCurrentImplementationId())) //Skip the metricsSnapshot if it is not related to the current implementation
                        continue;
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
                    } else {
                        shutdownInstancesStillMonitored.add(instance);
                    }
                }
                // Failure detection of instances
                if (!previouslyActiveInstances.isEmpty()) {
                    Set<Instance> failedInstances = new HashSet<>(previouslyActiveInstances);
                    failedInstances.removeAll(currentlyActiveInstances);
                    failedInstances.removeIf(instance -> instance.getCurrentStatus() == InstanceStatus.SHUTDOWN);

                    // There should be only the instances that have been shutdown and are still monitored
                    failedInstances.forEach(instance -> {
                        instance.setCurrentStatus(InstanceStatus.FAILED);
                        InstanceMetricsSnapshot metrics = new InstanceMetricsSnapshot(instance.getServiceId(), instance.getInstanceId());
                        metrics.setStatus(InstanceStatus.FAILED);
                        metrics.applyTimestamp();
                        metricsRepository.save(metrics);
                        instance.setLatestInstanceMetricsSnapshot(metrics);
                    });
                }
                previouslyActiveInstances = new HashSet<>(currentlyActiveInstances);
            }
            // For each service, remove from the map of instances the instances that have been shutdown that are not monitored anymore
            for (Service service : servicesMap.values()) {
                Set<Instance> instancesToBeRemoved = new HashSet<>(service.getShutdownInstances());
                instancesToBeRemoved.removeAll(shutdownInstancesStillMonitored);
                for (Instance instance : instancesToBeRemoved) {
                    service.removeInstance(instance);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void shutdownInstance(String serviceId, String instanceId) {
        Service service = servicesMap.get(serviceId);
        Instance instance = service.getInstance(instanceId);
        InstanceMetricsSnapshot metrics = new InstanceMetricsSnapshot(instance.getServiceId(), instance.getInstanceId());
        metrics.setStatus(InstanceStatus.SHUTDOWN);
        metrics.applyTimestamp();
        metricsRepository.save(metrics);
        instance.setCurrentStatus(InstanceStatus.SHUTDOWN);
        instance.setLatestInstanceMetricsSnapshot(metrics);
    }

    public void changeServiceImplementation(String serviceId, String newImplementationId, List<String> newInstancesAddresses){
        Service service = servicesMap.get(serviceId);
        service.getCurrentImplementation().setPenalty(0);

        for(Instance instance : service.getInstances()){
            shutdownInstance(serviceId, instance.getInstanceId());
            service.removeInstance(instance);
        }
        service.setCurrentImplementationId(newImplementationId);

        for(String instanceAddress : newInstancesAddresses) {
            service.createInstance(instanceAddress);
        }

        if(service.getConfiguration().getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM){
            Map<String, Double> newWeights = new HashMap<>();
            for(Instance instance : service.getInstances()){
                newWeights.put(instance.getInstanceId(), 1.0/service.getInstances().size());
            }
            setLoadBalancerWeights(serviceId, newWeights);
        }
    }

    public void addInstance(String serviceId, String instanceAddress){
        Service service = servicesMap.get(serviceId);
        service.createInstance(instanceAddress);
    }

    public InstanceMetricsSnapshot getMetrics(long id) {
        return metricsRepository.findById(id).orElse(null);
    }

    public void changeServicesConfigurations(Map<String, ServiceConfiguration> newConfigurations){
        for (String serviceId : newConfigurations.keySet()){
            Service service = servicesMap.get(serviceId);
            service.setConfiguration(newConfigurations.get(serviceId));
            configurationRepository.save(newConfigurations.get(serviceId));
        }
    }



    public List<InstanceMetricsSnapshot> getLatestNMetricsOfCurrentInstance(String serviceId, String instanceId, int n) {
        return metricsRepository.findLatestOfCurrentInstanceOrderByTimestampDesc(instanceId, servicesMap.get(serviceId).getLatestAdaptationDate(), Pageable.ofSize(n)).stream().toList();
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
        return servicesMap.get(serviceId);
    }

    public List<AdaptationOption> getChosenAdaptationOptionsHistory(String serviceId, int n) {
        return adaptationChoicesRepository.findAllByServiceIdOrderByTimestampDesc(serviceId, Pageable.ofSize(n)).stream().toList();
    }

    public Map<String, List<AdaptationOption>> getChosenAdaptationOptionsHistory(int n) {
        return adaptationChoicesRepository.findAll(Pageable.ofSize(n)).stream().collect(Collectors.groupingBy(AdaptationOption::getServiceId));
    }

    public void proposeAdaptationOptions(Map<String, List<AdaptationOption>> proposedAdaptationOptions) {
        this.proposedAdaptationOptions = proposedAdaptationOptions;
        for (String serviceId : proposedAdaptationOptions.keySet()) {
            Service service = servicesMap.get(serviceId);
            service.getCurrentImplementation().incrementPenalty();
        }
    }

    // Called by the Plan module to choose the adaptation options
    public void chooseAdaptationOptions(Map<String, List<AdaptationOption>> chosenAdaptationOptions) {
        // add the options both to the repository and to the map
        this.chosenAdaptationOptions = chosenAdaptationOptions;
        this.chosenAdaptationOptions.values().forEach(serviceOptions -> {
            serviceOptions.forEach(option -> {
                option.applyTimestamp();
                adaptationChoicesRepository.save(option);
            });
        });
    }


    // Update QoS-related properties
    public void addNewInstanceQoSValue(String serviceId, String instanceId, Class<? extends QoSSpecification> qosClass, Double value) {
        servicesMap.get(serviceId).getInstance(instanceId).getQoSCollection().addNewQoSValue(qosClass, value);
    }

    public void addNewServiceQoSValue(String serviceId, Class<? extends QoSSpecification> qosClass, Double value) {
        servicesMap.get(serviceId).getCurrentImplementation().getQoSCollection().addNewQoSValue(qosClass, value);
    }

    public void updateServiceQoSCollection(String serviceId, QoSCollection qoSCollection) {
        servicesMap.get(serviceId).getCurrentImplementation().setQoSCollection(qoSCollection);
    }

    public void updateInstanceQoSCollection(String serviceId, String instanceId, QoSCollection qoSCollection) {
        servicesMap.get(serviceId).getInstance(instanceId).setQoSCollection(qoSCollection);
    }

    public void updateService(Service service) {
        servicesMap.put(service.getServiceId(), service);
    }




    public void updateBenchmark(String serviceId, String serviceImplementationId, Class<? extends QoSSpecification> qosClass, Double value) { //TODO è thread safe?
        servicesMap.get(serviceId).getPossibleImplementations().get(serviceImplementationId).getQoSBenchmarks().put(qosClass, value);
    }

    public void setLoadBalancerWeights(String serviceId, Map<String, Double> weights) { // serviceId, Map<instanceId, weight>
        Service service = servicesMap.get(serviceId);
        ServiceConfiguration oldConfiguration = service.getConfiguration();
        ServiceConfiguration newConfiguration = new ServiceConfiguration();
        newConfiguration.setLoadBalancerType(oldConfiguration.getLoadBalancerType());
        newConfiguration.setLoadBalancerWeights(weights);
        newConfiguration.setServiceId(serviceId);
        newConfiguration.setCircuitBreakersConfiguration(oldConfiguration.getCircuitBreakersConfiguration());
        newConfiguration.setTimestamp(new Date());
        service.setConfiguration(newConfiguration);
        configurationRepository.save(service.getConfiguration());
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
        Service service = servicesMap.get(serviceId);
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