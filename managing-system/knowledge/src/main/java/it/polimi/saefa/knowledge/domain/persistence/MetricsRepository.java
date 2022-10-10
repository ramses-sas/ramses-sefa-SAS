package it.polimi.saefa.knowledge.domain.persistence;

import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;


public interface MetricsRepository extends CrudRepository<InstanceMetricsSnapshot, Long> {
    Timestamp MIN_TIMESTAMP = new Timestamp(0);
    String SHUTDOWNSTATUS = "it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.SHUTDOWN";

    Collection<InstanceMetricsSnapshot> findAllByInstanceId(String instanceId);

    Collection<InstanceMetricsSnapshot> findAllByTimestampBetween(Date start, Date end);

    Collection<InstanceMetricsSnapshot> findAllByInstanceIdAndTimestampBetween(String instanceId, Date start, Date end);

    // InstanceMetricsSnapshot findByServiceIdAndInstanceIdAndTimestamp(String serviceId, String instanceId, Date timestamp);

    @Query("SELECT m FROM InstanceMetricsSnapshot m WHERE m.instanceId = :instanceId AND " +
            "m.timestamp > IFNULL((SELECT MAX(m1.timestamp) FROM InstanceMetricsSnapshot m1 WHERE m1.instanceId = :instanceId AND m.status="+SHUTDOWNSTATUS+"), it.polimi.saefa.knowledge.domain.persistence.MetricsRepository.MIN_TIMESTAMP) " +
            "AND m.timestamp >= :after ORDER BY m.timestamp DESC")
    Page<InstanceMetricsSnapshot> findLatestOfCurrentInstanceOrderByTimestampDesc(String instanceId, Date after, Pageable pageable);

    Page<InstanceMetricsSnapshot> findAllByInstanceIdAndTimestampBeforeOrderByTimestampDesc(String instanceId, Date timestamp, Pageable pageable);

    Page<InstanceMetricsSnapshot> findAllByInstanceIdAndTimestampAfterOrderByTimestampDesc(String instanceId, Date timestamp, Pageable pageable);

    @Query("SELECT m FROM InstanceMetricsSnapshot m WHERE m.instanceId = :instanceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetricsSnapshot m2 WHERE m2.instanceId = :instanceId)")
    Collection<InstanceMetricsSnapshot> findLatestByInstanceId(@Param("instanceId") String instanceId);

    @Query("SELECT m FROM InstanceMetricsSnapshot m WHERE m.serviceId = :serviceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetricsSnapshot m2 WHERE m2.serviceId = :serviceId and m2.instanceId = m.instanceId)")
    Collection<InstanceMetricsSnapshot> findLatestByServiceId(String serviceId);

    // = it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.
    @Query("SELECT m FROM InstanceMetricsSnapshot m WHERE m.instanceId = :instanceId AND m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE " +
            "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetricsSnapshot m2 WHERE m2.instanceId = :instanceId)")
    Collection<InstanceMetricsSnapshot> findLatestOnlineMeasurementByInstanceId(String instanceId);

    @Query("SELECT m FROM InstanceMetricsSnapshot m WHERE m.instanceId = :instanceId AND m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetricsSnapshot m2 WHERE m2.instanceId = :instanceId) AND m.timestamp < (SELECT MAX(m3.timestamp) FROM InstanceMetricsSnapshot m3 WHERE m3.instanceId = :instanceId AND m3.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED)")
    Collection<InstanceMetricsSnapshot> findLatestOnlineMeasurementIfDownByInstanceId(String instanceId);

    @Query("SELECT m FROM InstanceMetricsSnapshot m WHERE m.instanceId = :instanceId " +
            "AND (m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED " +
            "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetricsSnapshot m2 WHERE m2.instanceId = :instanceId AND m2.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED) " +
            "OR (m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE AND m.timestamp = (SELECT MIN(m3.timestamp) FROM InstanceMetricsSnapshot m3 WHERE m3.instanceId = :instanceId AND m3.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE AND m3.timestamp> (SELECT MAX(m4.timestamp) FROM InstanceMetricsSnapshot m4 WHERE m4.instanceId = :instanceId AND m4.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED))))")
    Collection<InstanceMetricsSnapshot> findLatestDowntimeByServiceId(String instanceId);
    //Selects the most recent "down" metric and, if present, the "online" metric that follows it (the one with the smallest timestamp greater than the "down" one)
}

