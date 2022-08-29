package it.polimi.saefa.knowledge.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersistenceService {
    @Autowired
    private MetricsRepository metricsRepository;

    public void addMetrics(InstanceMetrics metrics) {
        metricsRepository.save(metrics);
    }

    public InstanceMetrics getMetrics(String instanceId) {
        return metricsRepository.orElse(null);
    }


}
