package it.polimi.saefa.knowledge.persistence;

import it.polimi.saefa.knowledge.persistence.components.CircuitBreakerMetrics;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;

public interface MetricsRepository extends CrudRepository<InstanceMetrics, Long> {
    Collection<InstanceMetrics> findAllByInstanceId(String instanceId);
    Collection<InstanceMetrics> findAllByServiceId(String serviceId);
    Collection<InstanceMetrics> findAllByTimestampBetween(Date start, Date end);

    InstanceMetrics findByInstanceIdAndTimestamp(String instanceId, Date timestamp);
    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId)")
    InstanceMetrics findLatestByInstanceId(@Param("instanceId") String instanceId);
    @Query("SELECT m FROM InstanceMetrics m WHERE m.serviceId = :serviceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.serviceId = :serviceId and m2.instanceId = m.instanceId)")
    Collection<InstanceMetrics> findLatestByServiceId(String serviceId);

}

