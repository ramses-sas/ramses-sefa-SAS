package it.polimi.saefa.knowledge.domain.persistence;

import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;


public interface MetricsRepository extends CrudRepository<InstanceMetrics, Long> {
    Timestamp MIN_TIMESTAMP = new Timestamp(0);
    String SHUTDOWNSTATUS = "it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.SHUTDOWN";

    Collection<InstanceMetrics> findAllByInstanceId(String instanceId);

    Collection<InstanceMetrics> findAllByTimestampBetween(Date start, Date end);

    Collection<InstanceMetrics> findAllByInstanceIdAndTimestampBetween(String instanceId, Date start, Date end);

    // InstanceMetrics findByServiceIdAndInstanceIdAndTimestamp(String serviceId, String instanceIdList, Date timestamp);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND " +
            "m.timestamp > ISNULL((SELECT MAX(m1.timestamp) FROM InstanceMetrics m1 WHERE m1.instanceId = :instanceId AND m.status="+SHUTDOWNSTATUS+"), it.polimi.saefa.knowledge.domain.persistence.MetricsRepository.MIN_TIMESTAMP) " +
            "ORDER BY m.timestamp DESC")
    Page<InstanceMetrics> findLatestOfCurrentInstanceOrderByTimestampDesc(String instanceId, Pageable pageable); //TODO vanno restituite quelle dopo il timestamp dell'ultima applied (non più chosen) adaptation option

    Page<InstanceMetrics> findAllByInstanceIdAndTimestampBeforeOrderByTimestampDesc(String instanceId, Date timestamp, Pageable pageable);

    Page<InstanceMetrics> findAllByInstanceIdAndTimestampAfterOrderByTimestampDesc(String instanceId, Date timestamp, Pageable pageable);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId)")
    Collection<InstanceMetrics> findLatestByInstanceId(@Param("instanceId") String instanceId);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.serviceId = :serviceId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.serviceId = :serviceId and m2.instanceId = m.instanceId)")
    Collection<InstanceMetrics> findLatestByServiceId(String serviceId);

    // = it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.
    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE " +
            "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId)")
    Collection<InstanceMetrics> findLatestOnlineMeasurementByInstanceId(String instanceId);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId AND m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId) AND m.timestamp < (SELECT MAX(m3.timestamp) FROM InstanceMetrics m3 WHERE m3.instanceId = :instanceId AND m3.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED)")
    Collection<InstanceMetrics> findLatestOnlineMeasurementIfDownByInstanceId(String instanceId);

    @Query("SELECT m FROM InstanceMetrics m WHERE m.instanceId = :instanceId " +
            "AND (m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED " +
            "AND m.timestamp = (SELECT MAX(m2.timestamp) FROM InstanceMetrics m2 WHERE m2.instanceId = :instanceId AND m2.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED) " +
            "OR (m.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE AND m.timestamp = (SELECT MIN(m3.timestamp) FROM InstanceMetrics m3 WHERE m3.instanceId = :instanceId AND m3.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.ACTIVE AND m3.timestamp> (SELECT MAX(m4.timestamp) FROM InstanceMetrics m4 WHERE m4.instanceId = :instanceId AND m4.status=it.polimi.saefa.knowledge.domain.architecture.InstanceStatus.FAILED))))")
    Collection<InstanceMetrics> findLatestDowntimeByServiceId(String instanceId);
    //Selects the most recent "down" metric and, if present, the "online" metric that follows it (the one with the smallest timestamp greater than the "down" one)
}

