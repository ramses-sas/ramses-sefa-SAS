package polimi.saefa.orderingservice.rest;


import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import polimi.saefa.orderingservice.domain.*;
import polimi.saefa.orderingservice.restapi.common.*;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path="/rest/")
public class OrderingRestController {

	@Autowired
	private OrderingService orderingService;
	
    private final Logger logger = Logger.getLogger(OrderingRestController.class.toString());

	@PostMapping(path = "addItem")
	public AddItemToCartResponse addItemToCart(@RequestBody AddItemToCartRequest request){

		logger.info("REST CALL: addItem to cart " + request.getCartId() + " for restaurant " + request.getRestaurantId() + " item " + request.getItemId() + " * " + request.getQuantity());

		Cart cart = orderingService.addItemToCart(request.getCartId(), request.getRestaurantId(), request.getItemId(), request.getQuantity());

		orderingService.updateCartPrice(cart);
		List<CartItemElement> cartItemElements =
				cart.getItemList()
						.stream()
						.map(this::cartItemToCartItemElement)
						.collect(Collectors.toList());
		return new AddItemToCartResponse(cart.getId(), cart.getRestaurantId(), cart.getTotalPrice(), cartItemElements);

	}

	@PostMapping(path = "removeItem")
	public RemoveItemFromCartResponse removeItemFromCart(@RequestBody RemoveItemFromCartRequest request){

		logger.info("REST CALL: removeItem to cart " + request.getCartId() + " for restaurant " + request.getRestaurantId() + " item " + request.getItemId() + " * " + request.getItemId());

		Cart cart = orderingService.removeItemFromCart(request.getCartId(), request.getRestaurantId(), request.getItemId(), request.getQuantity());

		orderingService.updateCartPrice(cart);
			List<CartItemElement> cartItemElements =
					cart.getItemList()
							.stream()
							.map(this::cartItemToCartItemElement)
							.collect(Collectors.toList());
		return new RemoveItemFromCartResponse(cart.getId(), cart.getRestaurantId(), cart.getTotalPrice(), cartItemElements);

	}

	@PostMapping(path = "confirmOrder")
	public ConfirmOrderResponse confirmOrder(@RequestBody ConfirmOrderRequest request){
		PaymentInfo paymentInfo;
		try{
			paymentInfo = new PaymentInfo(request.getCardNumber(), request.getExpMonth(), request.getExpYear(), request.getCvv());
		} catch (PaymentInfo.PaymentDetailsNotValidException e){
			return new ConfirmOrderResponse();
		}
		DeliveryInfo deliveryInfo = new DeliveryInfo(request.getAddress(), request.getCity(), request.getNumber(), request.getZipcode(), request.getTelephoneNumber(), request.getScheduledTime());

		if(orderingService.processPayment(request.getCartId(), paymentInfo))
			orderingService.processDelivery(request.getCartId(), deliveryInfo);

		return new ConfirmOrderResponse();

	}

	private CartItemElement cartItemToCartItemElement(CartItem item) {
		return new CartItemElement(item.getId(), item.getQuantity());
	}
}
