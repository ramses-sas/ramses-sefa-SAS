package it.polimi.ramses.knowledge.domain.persistence;

import it.polimi.ramses.knowledge.domain.architecture.ServiceConfiguration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepository extends CrudRepository<ServiceConfiguration, Long> {

}
