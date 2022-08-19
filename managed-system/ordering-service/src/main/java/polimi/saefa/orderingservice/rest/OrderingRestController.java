package polimi.saefa.orderingservice.rest;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import polimi.saefa.orderingservice.domain.*;
import polimi.saefa.orderingservice.exceptions.ConfirmOrderException;
import polimi.saefa.orderingservice.restapi.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path="/rest")
public class OrderingRestController {

	@Autowired
	private OrderingService orderingService;
	
    private final Logger logger = Logger.getLogger(OrderingRestController.class.toString());

	@PostMapping(path = "/")
	public CreateCartResponse createCart(@RequestBody CreateCartRequest request) {
		Cart cart = orderingService.createCart(request.getRestaurantId());
		return new CreateCartResponse(cart.getId(), cart.getRestaurantId());
	}

	@PostMapping(path = "/{cartId}/addItem")
	public AddItemToCartResponse addItemToCart(@PathVariable Long cartId, @RequestBody AddItemToCartRequest request){

		logger.info("REST CALL: addItem to cart " + cartId + " for restaurant " + request.getRestaurantId() + " item " + request.getItemId() + " * " + request.getQuantity());

		Cart cart = orderingService.addItemToCart(cartId, request.getRestaurantId(), request.getItemId(), request.getQuantity());

		List<CartItemElement> cartItemElements =
				cart.getItemList()
						.stream()
						.map(this::cartItemToCartItemElement).toList();
		return new AddItemToCartResponse(cart.getId(), cart.getRestaurantId(), cart.getTotalPrice(), cartItemElements);
	}

	@PostMapping(path = "/{cartId}/removeItem")
	public RemoveItemFromCartResponse removeItemFromCart(@PathVariable Long cartId, @RequestBody RemoveItemFromCartRequest request) {

		logger.info("REST CALL: removeItem to cart " + cartId + " for restaurant " + request.getRestaurantId() + " item " + request.getItemId() + " * " + request.getItemId());

		Cart cart = orderingService.removeItemFromCart(cartId, request.getRestaurantId(), request.getItemId(), request.getQuantity());

		List<CartItemElement> cartItemElements =
				cart.getItemList()
						.stream()
						.map(this::cartItemToCartItemElement)
						.collect(Collectors.toList());
		return new RemoveItemFromCartResponse(cart.getId(), cart.getRestaurantId(), cart.getTotalPrice(), cartItemElements);
	}

	@PostMapping(path = "/{cartId}/confirmOrder")
	public ConfirmOrderResponse confirmOrder(@PathVariable Long cartId, @RequestBody ConfirmOrderRequest request) {
		logger.info("REST CALL: confirmOrder to cart " + cartId);

		PaymentInfo paymentInfo = new PaymentInfo(request.getCardNumber(), request.getExpMonth(), request.getExpYear(), request.getCvv());
		DeliveryInfo deliveryInfo = new DeliveryInfo(request.getAddress(), request.getCity(), request.getNumber(), request.getZipcode(), request.getTelephoneNumber(), request.getScheduledTime());

		if (orderingService.processPayment(cartId, paymentInfo) && orderingService.processDelivery(cartId, deliveryInfo))
			return new ConfirmOrderResponse(orderingService.notifyRestaurant(cartId, false));
		else
			throw new ConfirmOrderException("Payment or delivery processing failed");
	}

	@PatchMapping(path = "/{cartId}/confirmCashPayement")
	public ConfirmOrderResponse confirmCashPayment(@PathVariable Long cartId, @RequestBody ConfirmCashPaymentRequest request) {
		logger.info("REST CALL: confirmCashPayment to cart " + cartId);

		DeliveryInfo deliveryInfo = new DeliveryInfo(request.getAddress(), request.getCity(), request.getNumber(), request.getZipcode(), request.getTelephoneNumber(), request.getScheduledTime());

		if (orderingService.confirmCashPayment(cartId) && orderingService.processDelivery(cartId, deliveryInfo))
			return new ConfirmOrderResponse(orderingService.notifyRestaurant(cartId, false), true, false);
		else
			throw new ConfirmOrderException("Payment or delivery processing failed");
	}

	@PatchMapping(path = "/{cartId}/confirmTakeAway")
	public ConfirmOrderResponse confirmTakeAway(@PathVariable Long cartId) {
		logger.info("REST CALL: confirmTakeAway to cart " + cartId);
		return new ConfirmOrderResponse(orderingService.notifyRestaurant(cartId, true), orderingService.orderRequiresCashPayment(cartId), orderingService.orderRequiresTakeaway(cartId));

	}
	@PatchMapping(path = "/{cartId}/rejectTakeAway")
	public ConfirmOrderResponse rejectTakeAway(@PathVariable Long cartId) {
		logger.info("REST CALL: rejectTakeAway to cart " + cartId);
		return new ConfirmOrderResponse(false);
		// TODO dummy just for doing it
	}


	@GetMapping (path = "/{cartId}")
	public GetCartResponse getCart(@PathVariable Long cartId) {
		logger.info("REST CALL: getCart for cart " + cartId);
		Cart cart = orderingService.getCart(cartId);
		List<CartItemElement> cartItemElements =
				cart.getItemList()
						.stream()
						.map(this::cartItemToCartItemElement).toList();
		return new GetCartResponse(cart.getId(), cart.getRestaurantId(), cart.getTotalPrice(), cartItemElements);
	}

	private CartItemElement cartItemToCartItemElement(CartItem item) {
		return new CartItemElement(item.getId(), item.getName(), item.getPrice(), item.getQuantity());
	}
}
