package it.polimi.saefa.knowledge.domain.persistence;

import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface AdaptationChoicesRepository extends CrudRepository<AdaptationOption, Long> {
    @Query("SELECT m FROM AdaptationOption m WHERE m.serviceImplementationId = :serviceImplementationId AND m.timestamp = (SELECT MAX(m2.timestamp) FROM AdaptationOption m2 WHERE m2.serviceImplementationId = :serviceImplementationId)")
    AdaptationOption getLatestByServiceImplementationId(String serviceImplementationId);

    Page<AdaptationOption> findAllByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

    @Query("SELECT m FROM AdaptationOption m ORDER BY m.timestamp DESC")
    Page<AdaptationOption> findAll(Pageable pageable);

}
