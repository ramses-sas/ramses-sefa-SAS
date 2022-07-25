package polimi.saefa.orderingservice.domain;


import org.springframework.data.repository.CrudRepository;

import java.util.*;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderingRepository extends CrudRepository<Cart, Long> {

}

