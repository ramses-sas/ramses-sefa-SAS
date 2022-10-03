package it.polimi.saefa.knowledge.domain;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
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
    private final Set<Instance> shutdownInstances = Collections.synchronizedSet(new HashSet<>());
    // <serviceId, AdaptationOptions proposed by the Analyse>
    @Getter @Setter
    private Map<String, List<AdaptationOption>> proposedAdaptationOptions = new HashMap<>();
    // <serviceId, AdaptationOption chosen by the Plan>
    @Getter @Setter
    private Map<String, AdaptationOption> chosenAdaptationOptions = new HashMap<>();



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

    public void addMetrics(List<InstanceMetrics> metricsList) {
        log.info("Saving new set of metrics");
        Set<Instance> currentlyActiveInstances = new HashSet<>();

        metricsList.forEach(metrics -> {
            Service service = services.get(metrics.getServiceId()); //TODO l'executor deve notificare la knowledge quando un servizio cambia il microservizio che lo implementa
            Instance instance = service.getOrCreateInstance(metrics.getInstanceId());
            if (instance != null) {
                if(!(instance.getLastMetrics() != null && instance.getLastMetrics().equals(metrics))) {
                    metricsRepository.save(metrics);
                    instance.setLastMetrics(metrics);
                    instance.setCurrentStatus(metrics.getStatus());
                } else
                    log.warn("Metrics already saved: " + metrics);
                if (metrics.isActive())
                    currentlyActiveInstances.add(instance);
            }
        });

        if (previouslyActiveInstances.isEmpty())
            previouslyActiveInstances.addAll(currentlyActiveInstances);
        else {
            Set<Instance> failedInstances = new HashSet<>(previouslyActiveInstances);
            failedInstances.removeAll(currentlyActiveInstances);
            failedInstances.removeAll(shutdownInstances);
            for(Instance shutdownInstance : shutdownInstances){
                if(!currentlyActiveInstances.contains(shutdownInstance)) {
                    // the instance has been correctly shutdown
                    shutdownInstances.remove(shutdownInstance);
                    shutdownInstance.setCurrentStatus(InstanceStatus.SHUTDOWN);
                    services.get(shutdownInstance.getServiceId()).removeInstance(shutdownInstance);
                    InstanceMetrics metrics = new InstanceMetrics(shutdownInstance.getServiceId(), shutdownInstance.getInstanceId());
                    metrics.setStatus(InstanceStatus.SHUTDOWN);
                    metrics.applyTimestamp();
                    //shutdownInstance.addMetric(metrics);
                    metricsRepository.save(metrics);
                }
            }
            //shutdownInstances.removeIf(instance -> !currentlyActiveInstances.contains(instance)); //if the instance has been shut down and cannot be contacted from the monitor,
            //it won't be reached from the monitor in the future, thus it can be removed from the set of shutdown instances TODO ^è già fatto nel foreach, no?

            /*Caso risolto utilizzando la riga di codice precedente
                //shutDownInstances.clear(); Non possiamo pulire questo set. Questo perché magari eureka non rimuove
                // subito l'istanza spenta e al prossimo aggiornamento verrebbe contata come crashata. L'informazione va
                // mantenuta nel set finché la macchina non viene riaccesa, nel caso. Quindi se una macchina viene
                // (ri)accesa, il Knowledge deve saperlo, per poterla eventualmente rimuovere da shutdownInstances.
            */

            failedInstances.forEach(instance -> {
                instance.setCurrentStatus(InstanceStatus.FAILED);
                InstanceMetrics metrics = new InstanceMetrics(instance.getServiceId(), instance.getInstanceId());
                metrics.setStatus(InstanceStatus.FAILED);
                metrics.applyTimestamp();
                metricsRepository.save(metrics);
            } );
            previouslyActiveInstances = new HashSet<>(currentlyActiveInstances);
        }
        currentlyActiveInstances.clear();
    }

    public void notifyShutdownInstance(Instance instance) {
        shutdownInstances.add(instance);
        //Codice rimosso perché può succedere che avvio lo shutdown di una macchina ma ricevo successivamente una
        // richiesta dal monitor che la contiene ancora.
        /*InstanceMetrics metrics = new InstanceMetrics(serviceId,instanceIdList);
        metrics.setStatus(InstanceStatus.SHUTDOWN);
        metrics.applyTimestamp();
        metricsRepository.save(metrics);
        */
    }

    public InstanceMetrics getMetrics(long id) {
        return metricsRepository.findById(id).orElse(null);
    }

    public void changeServicesConfigurations(Map<String, ServiceConfiguration> newConfigurations){
        for (String serviceId : newConfigurations.keySet()){
            Service service = services.get(serviceId);
            service.setConfiguration(newConfigurations.get(serviceId));
            configurationRepository.save(newConfigurations.get(serviceId));
        }
    }

    public List<InstanceMetrics> getAllInstanceMetrics(String instanceId) {
        return metricsRepository.findAllByInstanceId(instanceId).stream().toList();
    }

    public List<InstanceMetrics> getAllMetricsBetween(String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByTimestampBetween(startDate, endDate).stream().toList();
    }

    public List<InstanceMetrics> getNMetricsBefore(String instanceId, String timestampStr, int n) {
        Date timestamp = Date.from(LocalDateTime.parse(timestampStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampBeforeOrderByTimestampDesc(instanceId, timestamp, Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetrics> getNMetricsAfter(String instanceId, String timestampStr, int n) {
        Date timestamp = Date.from(LocalDateTime.parse(timestampStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampAfterOrderByTimestampDesc(instanceId, timestamp, Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetrics> getLatestNMetricsOfCurrentInstance(String instanceId, int n) {
        return metricsRepository.findLatestOfCurrentInstanceOrderByTimestampDesc(instanceId, Pageable.ofSize(n)).stream().toList();
    }

    public List<InstanceMetrics> getAllInstanceMetricsBetween(String instanceId, String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByInstanceIdAndTimestampBetween(instanceId, startDate, endDate).stream().toList();
    }

    public InstanceMetrics getLatestByInstanceId(String instanceId) {
        return metricsRepository.findLatestByInstanceId(instanceId).stream().findFirst().orElse(null);
    }

    public InstanceMetrics getLatestActiveByInstanceId(String instanceId) {
        return metricsRepository.findLatestOnlineMeasurementByInstanceId(instanceId).stream().findFirst().orElse(null);
    }

    public List<InstanceMetrics> getAllLatestByServiceId(String serviceId) {
        return metricsRepository.findLatestByServiceId(serviceId).stream().toList();
    }

    public Service getService(String serviceId) {
        return services.get(serviceId);
    }

    public List<AdaptationOption> getAdaptationOptions(String serviceId, int n) {
        return adaptationChoicesRepository.findAllByServiceIdOrderByTimestampDesc(serviceId, Pageable.ofSize(n)).stream().toList();
    }

    // Called by the Plan module to choose the adaptation options
    public void chooseAdaptationOptions(List<AdaptationOption> options) {
        // a new loop is started: reset the previous chosen options and the current proposed adaptation options
        proposedAdaptationOptions = new HashMap<>();
        chosenAdaptationOptions = new HashMap<>();
        // add the options both to the repository and to the map
        options.forEach(option -> {
            // option.applyTimestamp(); MOVE to the method called by the Plan module
            adaptationChoicesRepository.save(option);
            chosenAdaptationOptions.put(option.getServiceId(), option);
        });
    }

    public void addNewInstanceAdaptationParameterValue(String serviceId, String instanceId, Class<? extends AdaptationParamSpecification> adaptationParameterClass, Double value) {
        services.get(serviceId).getOrCreateInstance(instanceId).getAdaptationParamCollection().addNewAdaptationParamValue(adaptationParameterClass, value);
    }

    public void addNewServiceAdaptationParameterValue(String serviceId, Class<? extends AdaptationParamSpecification> adaptationParameterClass, Double value) {
        services.get(serviceId).getCurrentImplementationObject().getAdaptationParamCollection().addNewAdaptationParamValue(adaptationParameterClass, value);
    }

    public void setLoadBalancerWeights(String serviceId, Map<String, Double> weights) { // serviceId, Map<instanceId, weight>
        Service service = services.get(serviceId);
        service.getConfiguration().setLoadBalancerWeights(weights);
        configurationRepository.save(service.getConfiguration());
    }

}



/*


    Non più necessario per l'inserimento della seguente riga di codice al metodo addMetrics:
    shutdownInstances.removeIf(instance -> !currentlyActiveInstances.contains(instance)); //if the instance has been shut down and cannot be contacted from the monitor,
    public void notifyBootInstance(String serviceId, String instanceIdList) {
        shutdownInstances.remove(serviceId + "@" + instanceIdList);
    }

    public InstanceMetrics getMetrics(String serviceId, String instanceIdList, String timestamp) {
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp);
        Date date = Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
        return metricsRepository.findByServiceIdAndInstanceIdAndTimestamp(serviceId, instanceIdList, date);
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