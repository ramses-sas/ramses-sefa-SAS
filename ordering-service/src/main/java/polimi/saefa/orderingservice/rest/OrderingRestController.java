package polimi.saefa.orderingservice.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import polimi.saefa.orderingservice.domain.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/")
public class OrderingRestController {

	@Autowired
	private OrderingService orderingService;
	
    private final Logger logger = Logger.getLogger(OrderingRestController.class.toString());


	@GetMapping("/test/{myString}")
	public String testOrdering(@PathVariable String myString) {
		logger.info("REST CALL: testOrdering " + myString);
		return orderingService.dummyMethod(myString);
	}

	@PostMapping(path = "addItem",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Cart> addItemToCart(@RequestBody Integer cartId, @RequestBody String restaurantId,
											  @RequestBody String itemId, @RequestBody int quantity,
											  @RequestBody List<CartItem> items){
		Cart cart;
		if (cartId == null || cartId==0){
			cart = new Cart(restaurantId);
		} else {
			cart = new Cart(cartId, restaurantId, items);
		}

		logger.info("REST CALL: addItem to cart " + cart + " for item " + itemId + " * " + quantity);

		if(orderingService.addItemToCart(cart, restaurantId, itemId, quantity)) {

			orderingService.updateCartPrice(cart);

			return new ResponseEntity<>(cart, HttpStatus.ACCEPTED);
		}
		else return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

	}

}
