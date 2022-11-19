package it.polimi.ramses.knowledge.domain.persistence;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;


@Transactional
public interface QoSRepository extends CrudRepository<QoSValueEntity, Long> {

    // invalidate QoSHistory for a given service and its instances
    @Modifying
    @Query(value =
            "update qosvalue_entity set invalidates_this_and_previous = true where id in (select id from (" +
                "select q.id from qosvalue_entity q where q.service_id = :serviceId and q.service_implementation_id = :implementationId and q.timestamp = (" +
                    "select max(q2.timestamp) from qosvalue_entity q2 where q2.service_id = :serviceId and q2.service_implementation_id = :implementationId and q2.qos = q.qos and (q2.instance_id = q.instance_id or (q.instance_id is null and q2.instance_id is null))" +
                ")" +
            ") as t)", nativeQuery = true)
    void invalidateServiceQoSHistory(String serviceId, String implementationId);

}
