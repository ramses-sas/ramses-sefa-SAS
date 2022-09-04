package it.polimi.saefa.knowledge.persistence;

import it.polimi.saefa.knowledge.persistence.domain.Instance;
import it.polimi.saefa.knowledge.persistence.domain.Service;
import it.polimi.saefa.knowledge.persistence.domain.ServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@org.springframework.stereotype.Service
public class PersistenceService {
    @Autowired
    private MetricsRepository metricsRepository;

    private Map<String, Service> services = new ConcurrentHashMap<>(); //va capito come vogliamo gestirlo, ovvero se
    // quando tutte le istanze di un servizio sono spente/crashate vogliamo che il servizio venga rimosso o meno

    //fare un file json che descrive l'architettura statica del managed system, con:
    //per i microservizi che rappresentano il servizio X abbiamo "lista di microservizi"

    private final Map<String, ServiceConfiguration> serviceConfigurationSet = new ConcurrentHashMap<>();
    private Set<String> previouslyActiveInstances = new HashSet<>();
    private final Set<String> shutdownInstances = Collections.synchronizedSet(new HashSet<>());

    /*public boolean addMetrics(Instance instance, InstanceMetrics metrics) {
        if(metrics.isActive() || getLatestByInstanceId(metrics.getServiceId(),metrics.getInstanceId()).isActive()) {
            //if the instance is down, only save it if it's the first detection
            instance.getMetrics().add(metrics);
            //metricsRepository.save(metrics);
            return true;
        }
        return false;
    }*/
    public boolean addMetrics(Instance instance, InstanceMetrics metrics) {
        if(metrics.isActive() || metrics.isShutdown() || getLatestByInstanceId(metrics.getServiceId(),metrics.getInstanceId()).isActive()) {
            //if the instance is down, only save it if it's the first detection
            instance.addMetric(metrics);
            metricsRepository.save(metrics);
            return true;
        }
        return false;
    }

    public void addMetrics(List<InstanceMetrics> metricsList) {
        Set<String> currentlyActiveInstances = new HashSet<>();

        metricsList.forEach(metrics -> {
            Service service = services.get(metrics.getServiceId());
            if(service == null){
                service = new Service(metrics.getServiceId(), metrics.getInstanceId());
                services.put(metrics.getServiceId(), service);
            }
            Instance instance = service.getOrCreateInstance(metrics.getInstanceId());
            addMetrics(instance, metrics);

            if(metrics.isActive())
                currentlyActiveInstances.add(metrics.getServiceId() + "@" + metrics.getInstanceId());
        } );

        if(previouslyActiveInstances.isEmpty())
            previouslyActiveInstances.addAll(currentlyActiveInstances);
        else {
            Set<String> failedInstances = new HashSet<>(previouslyActiveInstances);
            failedInstances.removeAll(currentlyActiveInstances);
            failedInstances.removeAll(shutdownInstances);
            for(String instanceId : shutdownInstances){
                if(!currentlyActiveInstances.contains(instanceId)) {
                    shutdownInstances.remove(instanceId);
                    String serviceName = instanceId.split("@")[0];
                    Service service = services.get(serviceName);
                    Instance instance = service.getOrCreateInstance(instanceId);
                    instance.setCurrentStatus(InstanceStatus.SHUTDOWN);
                    InstanceMetrics metrics = new InstanceMetrics(serviceName, instanceId);
                    metrics.setStatus(InstanceStatus.SHUTDOWN);
                    metrics.applyTimestamp();
                    instance.addMetric(metrics);
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

            failedInstances.forEach(instanceId -> {
                String serviceId = instanceId.split("@")[0];
                Service service = services.get(serviceId);
                Instance instance = service.getOrCreateInstance(instanceId);
                instance.setCurrentStatus(InstanceStatus.FAILED);
                InstanceMetrics metrics = new InstanceMetrics(serviceId, instanceId);
                metrics.setStatus(InstanceStatus.FAILED);
                metrics.applyTimestamp();
                metricsRepository.save(metrics);
            } );
            previouslyActiveInstances = new HashSet<>(currentlyActiveInstances);
        }
        currentlyActiveInstances.clear();

    }

    public void notifyShutdownInstance(String serviceId, String instanceId) {
        shutdownInstances.add(serviceId + "@" + instanceId);
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