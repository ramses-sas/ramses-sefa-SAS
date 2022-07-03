package polimi.saefa.orderingservice.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import polimi.saefa.orderingservice.domain.*;

import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/")
public class OrderingRestController {

	@Autowired
	private OrderingService orderingService;
	
    private final Logger logger = Logger.getLogger(OrderingRestController.class.toString());


	/* Trova il ristorante con restaurantId. */ 
	@GetMapping("/test/{myString}")
	public String testOrdering(@PathVariable String myString) {
		logger.info("REST CALL: testOrdering " + myString);
		return orderingService.dummyMethod(myString);
	}

}
