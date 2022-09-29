package it.polimi.saefa.knowledge.persistence;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;

public interface AdaptationChoicesRepository extends CrudRepository<AdaptationOption, Long> {
    @Query("SELECT m FROM AdaptationOption m WHERE m.serviceImplementationId = :serviceImplementationId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM AdaptationOption m2 WHERE m2.serviceImplementationId = :serviceImplementationId)")
    AdaptationOption getLatestByServiceImplementationId(String serviceImplementationId);

    Page<AdaptationOption> findAllByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

}
