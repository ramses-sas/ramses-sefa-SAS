package it.polimi.saefa.knowledge.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class PersistenceService {
    @Autowired
    private MetricsRepository metricsRepository;

    public void addMetrics(InstanceMetrics metrics) {
        metricsRepository.save(metrics);
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

    public List<InstanceMetrics> getAllLatestByServiceId(String serviceId) {
        return metricsRepository.findLatestByServiceId(serviceId).stream().toList();
    }


}


/*
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