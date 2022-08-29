package it.polimi.saefa.knowledge.persistence;

import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Date;

public interface MetricsRepository extends CrudRepository<InstanceMetrics, Long> {
    Collection<InstanceMetrics> findAllByInstanceId(String instanceId);
    Collection<InstanceMetrics> findAllByServiceId(String serviceId);
    Collection<InstanceMetrics> findAllByTimestampBetween(Date start, Date end);
}

