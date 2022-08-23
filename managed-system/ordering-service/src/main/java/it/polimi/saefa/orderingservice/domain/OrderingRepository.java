package it.polimi.saefa.orderingservice.domain;


import org.springframework.data.repository.CrudRepository;

public interface OrderingRepository extends CrudRepository<Cart, Long> {

}

