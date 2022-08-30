package it.polimi.saefa.knowledge.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
public class PersistenceService {
    @Autowired
    private MetricsRepository metricsRepository;

    public void addMetrics(InstanceMetrics metrics) {
        metricsRepository.save(metrics);
    }

    public List<InstanceMetrics> getMetrics() {
        List<InstanceMetrics> metrics = new LinkedList<>();
        metricsRepository.findAll().iterator().forEachRemaining(metrics::add);
        return metrics;
    }

    public List<InstanceMetrics> getMetrics(String instanceId) {
        List<InstanceMetrics> metrics = new LinkedList<>();
        metricsRepository.findAllByInstanceId(instanceId).iterator().forEachRemaining(metrics::add);
        return metrics;
    }

    public InstanceMetrics getMetrics(String instanceId, Date timestamp) {
        return metricsRepository.findByInstanceIdAndTimestamp(instanceId, timestamp);
    }

    public InstanceMetrics findLatestByInstanceId(String instanceId) {
        return metricsRepository.findLatestByInstanceId(instanceId);
    }

    public List<InstanceMetrics> findLatestByServiceId(String serviceId) {
        List<InstanceMetrics> metrics = new LinkedList<>();
        metricsRepository.findLatestByServiceId(serviceId).iterator().forEachRemaining(metrics::add);
        return metrics;
    }


}
