package it.polimi.saefa.knowledge.persistence;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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

    private final Map<String, Service> services = new ConcurrentHashMap<>();
    //va capito come vogliamo gestirlo, ovvero se
    // quando tutte le istanze di un servizio sono spente/crashate vogliamo che il servizio venga rimosso o meno
    // Scelta progettuale: una volta che un servizio è stato creato non viene mai rimosso, ma solo le sue istanze
    // (altrimenti è come il cambio di un functional requirement)

    private final Map<String, ServiceConfiguration> serviceConfigurationSet = new ConcurrentHashMap<>();
    private Set<Instance> previouslyActiveInstances = new HashSet<>();
    private final Set<Instance> shutdownInstances = Collections.synchronizedSet(new HashSet<>());

    /*public boolean addMetrics(Instance instance, InstanceMetrics metrics) {
        if(metrics.isActive() || getLatestByInstanceId(metrics.getServiceId(),metrics.getInstanceId()).isActive()) {
            //if the instance is down, only save it if it's the first detection
            instance.getMetrics().add(metrics);
            //metricsRepository.save(metrics);
            return true;
        }
        return false;
    }*/

    public void addService(Service service){
        services.put(service.getServiceId(), service);
    }

    public synchronized List<Service> getServices(){
        return services.values().stream().toList();
    }

    public Map<String,Service> getServicesMap() { return services; }
    
    public boolean addMetrics(Instance instance, InstanceMetrics metrics) {
        if(metrics.isActive() || metrics.isShutdown() || getLatestByInstanceId(metrics.getServiceId(),metrics.getInstanceId()).isActive()) {
            //if the instance is down, only save it if it's the first detection
            // TODO: questione metriche nell'oggetto istanza che diventa un oggettone. Soluzione: usare sempre e solo la repository
            //instance.addMetric(metrics);
            metricsRepository.save(metrics);
            return true;
        }
        return false;
    }

    public void addMetrics(List<InstanceMetrics> metricsList) {
        log.info("Saving new set of metrics");
        Set<Instance> currentlyActiveInstances = new HashSet<>();

        metricsList.forEach(metrics -> {
            Service service = services.get(metrics.getServiceId()); //TODO l'executor deve notificare la knowledge quando un servizio cambia il microservizio che lo implementa
            Instance instance = service.getOrCreateInstance(metrics.getInstanceId());
            service.setCurrentImplementation(metrics.getServiceImplementationName()); // TODO problema: al primo giro non sappiamo chi implementa i servizi
            addMetrics(instance, metrics);

            if (metrics.isActive())
                currentlyActiveInstances.add(instance);
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
                    InstanceMetrics metrics = new InstanceMetrics(shutdownInstance.getServiceId(), shutdownInstance.getInstanceId());
                    metrics.setStatus(InstanceStatus.SHUTDOWN);
                    metrics.applyTimestamp();
                    // TODO: questione metriche nell'oggetto istanza che diventa un oggettone. Soluzione: usare sempre e solo la repository
                    //shutdownInstance.addMetric(metrics);
                    metricsRepository.save(metrics);
                }
            }
            shutdownInstances.removeIf(instance -> !currentlyActiveInstances.contains(instance)); //if the instance has been shut down and cannot be contacted from the monitor,

            //it won't be reached from the monitor in the future, thus it can be removed from the set of shutdown instances

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
            /*if(service == null) { //TODO Non necessario se i servizi vengono inizializzati all'avvio.
                service = new Service();
                services.put(serviceId, service);
            }*/
            service.setConfiguration(newConfigurations.get(serviceId));
            configurationRepository.save(newConfigurations.get(serviceId));
        }
    }

    public List<InstanceMetrics> getAllInstanceMetrics(String serviceId, String instanceId) {
        return metricsRepository.findAllByServiceIdAndInstanceId(serviceId, instanceId).stream().toList();
    }

    public List<InstanceMetrics> getAllMetricsBetween(String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByTimestampBetween(startDate, endDate).stream().toList();
    }

    public List<InstanceMetrics> getAllInstanceMetricsBetween(String serviceId, String instanceId, String startDateStr, String endDateStr) {
        Date startDate = Date.from(LocalDateTime.parse(startDateStr).toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(LocalDateTime.parse(endDateStr).toInstant(ZoneOffset.UTC));
        return metricsRepository.findAllByServiceIdAndInstanceIdAndTimestampBetween(serviceId, instanceId, startDate, endDate).stream().toList();
    }

    public InstanceMetrics getLatestByInstanceId(String serviceId, String instanceId) {
        return metricsRepository.findLatestByServiceIdAndInstanceId(serviceId, instanceId);
    }

    public InstanceMetrics getLatestActiveByInstanceId(String instanceId) {
        return metricsRepository.findLatestOnlineMeasurementByInstanceId(instanceId);
    }

    public List<InstanceMetrics> getAllLatestByServiceId(String serviceId) {
        return metricsRepository.findLatestByServiceId(serviceId).stream().toList();
    }

    public boolean addInstanceConfigurationProperty(String serviceId, String property, String value) {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serviceId);
        return serviceConfigurationSet.put(serviceId,serviceConfiguration)!=null;
    }

    public Service getService(String serviceId) {
        return services.get(serviceId);
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