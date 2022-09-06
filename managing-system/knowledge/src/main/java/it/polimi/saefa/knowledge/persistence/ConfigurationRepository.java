package it.polimi.saefa.knowledge.persistence;

import it.polimi.saefa.knowledge.persistence.domain.ServiceConfiguration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepository extends CrudRepository<ServiceConfiguration, Long> {

}
