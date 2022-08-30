package it.polimi.saefa.knowledge.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    public InstanceMetrics getMetrics(long id) {
        return metricsRepository.findById(id).orElse(null);
    }

    public List<InstanceMetrics> getMetrics(String instanceId) {
        List<InstanceMetrics> metrics = new LinkedList<>();
        metricsRepository.findAllByInstanceId(instanceId).iterator().forEachRemaining(metrics::add);
        return metrics;
    }

    public InstanceMetrics getMetrics(String instanceId, String timestamp) {
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp);
        Date date = Date.from(localDateTime.atZone(ZoneOffset.UTC).toInstant());
        return metricsRepository.findByInstanceIdAndTimestamp(instanceId, date);
    }


}
