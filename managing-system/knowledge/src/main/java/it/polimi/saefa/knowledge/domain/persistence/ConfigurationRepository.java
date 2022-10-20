package it.polimi.saefa.knowledge.domain.persistence;

import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import org.springframework.data.repository.CrudRepository;

public interface ConfigurationRepository extends CrudRepository<ServiceConfiguration, Long> {

}
