package it.polimi.saefa.knowledge.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;

public interface MetricsRepository extends CrudRepository<InstanceMetrics, Long> {
    //Collection<InstanceMetrics> findAllByInstanceId(String instanceId);

    Collection<InstanceMetrics> findAllByServiceIdAndInstanceId(String serviceId, String instanceId);

    Collection<InstanceMetrics> findAllByTimestampBetween(Date start, Date end);

    Collection<InstanceMetrics> findAllByServiceIdAndInstanceIdAndTimestampBetween(String serviceId, String instanceId, Date start, Date end);

    InstanceMetrics findByServiceIdAndInstanceIdAndTimestamp(String serviceId, String instanceId, Date timestamp);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND m.serviceId = :serviceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId  AND m2.serviceId = :serviceId)")
    InstanceMetrics findLatestByServiceIdAndInstanceId(@Param("serviceId") String serviceId, @Param("instanceId") String instanceId);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.serviceId = :serviceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.serviceId = :serviceId and m2.instanceId = m.instanceId)")
    Collection<InstanceMetrics> findLatestByServiceId(String serviceId);
    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND m.isUp = TRUE AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId) AND m.timestamp < (SELECT MAX(m3.timestamp) FROM InstanceMetrics m3 WHERE m3.instanceId = :instanceId AND m3.isUp = FALSE)")
    InstanceMetrics findLatestOnlineMeasurementIfDownByInstanceId(String instanceId);

}

