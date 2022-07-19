package polimi.saefa.orderingservice.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import polimi.saefa.orderingservice.domain.*;
import polimi.saefa.orderingservice.restapi.common.*;
import polimi.saefa.restaurantservice.restapi.common.MenuItemElement;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

	@PostMapping(path = "addItem")
	public AddItemToCartResponse addItemToCart(@RequestBody AddItemToCartRequest request){

		logger.info("REST CALL: addItem to cart " + request.getCartId() + " for restaurant " + request.getRestaurantId() + " item " + request.getItemId() + " * " + request.getItemId());

		Cart cart = orderingService.addItemToCart(request.getCartId(), request.getRestaurantId(), request.getItemId(), request.getQuantity());

		if(orderingService.updateCartPrice(cart)) {
			List<CartItemElement> cartItemElements =
					cart.getItemList()
							.stream()
							.map(i -> cartItemToCartItemElement(i))
							.collect(Collectors.toList());
			return new AddItemToCartResponse(cart.getId(), cart.getRestaurantId(), cart.getTotalPrice(), cartItemElements);
		}
		else return null;
	}

	private CartItemElement cartItemToCartItemElement(CartItem item) {
		return new CartItemElement(item.getId(), item.getQuantity());
	}
}
