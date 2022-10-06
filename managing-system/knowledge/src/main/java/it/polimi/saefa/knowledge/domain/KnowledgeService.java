package it.polimi.saefa.knowledge.domain;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
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

    public void addMetrics(List<InstanceMetrics> metricsList) {
        log.info("Saving new set of metrics");
        Set<Instance> currentlyActiveInstances = new HashSet<>();

        metricsList.forEach(metrics -> {
            Service service = services.get(metrics.getServiceId()); //TODO l'executor deve notificare la knowledge quando un servizio cambia il microservizio che lo implementa
            Instance instance = service.getOrCreateInstance(metrics.getInstanceId());
            if(instance.getLatestMetrics() == null || !instance.getLatestMetrics().equals(metrics)) {//TODO la lista è fifo quindi non dobremmo avere problemi sulla lastMetrics, giusto? (nel senso che la last è X e nel buffer abbiamo Y Z K X)
                metricsRepository.save(metrics);
                instance.setLatestMetrics(metrics);
                instance.setCurrentStatus(metrics.getStatus());
            } else
                log.warn("Metrics already saved: " + metrics);
            if (metrics.isActive())
                currentlyActiveInstances.add(instance);

        });

        if (!previouslyActiveInstances.isEmpty()) {
            Set<Instance> failedInstances = new HashSet<>(previouslyActiveInstances);
            failedInstances.removeAll(currentlyActiveInstances);
            failedInstances.removeAll(shutdownInstances);
            for(Instance shutdownInstance : shutdownInstances){ //TODO capire
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
        }
        previouslyActiveInstances = new HashSet<>(currentlyActiveInstances);
    }

    public void notifyShutdownInstance(Instance instance) {
        shutdownInstances.add(instance);
        //Codice rimosso perché può succedere che avvio lo shutdown di una macchina ma ricevo successivamente una
        // richiesta dal monitor che la contiene ancora.
        /*InstanceMetrics metrics = new InstanceMetrics(serviceId,instanceId);
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
        return metricsRepository.findLatestOfCurrentInstanceOrderByTimestampDesc(instanceId, lastAdaptationDate, Pageable.ofSize(n)).stream().toList();
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

}



/*


    Non più necessario per l'inserimento della seguente riga di codice al metodo addMetrics:
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