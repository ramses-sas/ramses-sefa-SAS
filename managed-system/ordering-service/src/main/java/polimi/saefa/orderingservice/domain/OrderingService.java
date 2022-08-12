package polimi.saefa.orderingservice.domain;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimi.saefa.orderingservice.exceptions.CartNotFoundException;
import polimi.saefa.orderingservice.exceptions.ItemRemovalException;
import polimi.saefa.orderingservice.exceptions.MenuItemNotFoundException;
import polimi.saefa.orderingservice.externalInterfaces.*;
import polimi.saefa.orderingservice.rest.OrderingRestController;
import polimi.saefa.paymentproxyservice.restapi.*;
import polimi.saefa.deliveryproxyservice.restapi.*;
import polimi.saefa.restaurantservice.restapi.common.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@Transactional
public class OrderingService {
	@Autowired
	private OrderingRepository orderingRepository;
	@Autowired
	private RestaurantServiceClient restaurantServiceClient;
	@Autowired
	private DeliveryProxyClient deliveryProxyClient;
	@Autowired
	private PaymentProxyClient paymentProxyClient;

	private final CircuitBreakerRegistry circuitBreakerRegistry;
	public io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;

	private final Logger logger = Logger.getLogger(OrderingRestController.class.toString());

	public OrderingService(CircuitBreakerRegistry circuitBreakerRegistry) {
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderingService");
	}


	public Map<String, Number> check() {
		Map<String, Number> result = new HashMap<>();
		result.put("CircuitBreaker registry failureRateThreshold", circuitBreakerRegistry.getDefaultConfig().getFailureRateThreshold());
		result.put("CircuitBreaker failureRateThreshold", circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold());

		result.put(circuitBreaker.getState().toString(), 0);
		return result;
	}

	public Cart getCart(Long cartId) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()) return cart.get();
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public Cart createCart(Long restaurantId) {
		Cart cart = new Cart(restaurantId);
		return orderingRepository.save(cart);
	}

	public Cart addItemToCart(Long cartId, Long restaurantId, String item, int quantity) {
		 Cart cart = orderingRepository.findById(cartId).orElse(new Cart(restaurantId));

		 if (cart.addItem(item, restaurantId, quantity)) {
			 cart = updateCartDetails(cart);
		 }
		 return cart;
	}

	public Cart removeItemFromCart(Long cartId, Long restaurantId, String item, int quantity) {
		Optional<Cart> cart = orderingRepository.findById(cartId);

		if (cart.isPresent())
			if (cart.get().removeItem(item, restaurantId, quantity))
				return updateCartDetails(cart.get());
			else throw new ItemRemovalException("Impossible to remove the selected item from cart " + cartId);
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}
	public boolean notifyRestaurant(Long cartId) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent() && cart.get().isPaid()) {
			NotifyRestaurantResponse response = restaurantServiceClient.notifyRestaurant(cart.get().getRestaurantId(), cart.get().getId());
			return response.isNotified();
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	@CircuitBreaker(name = "orderingService", fallbackMethod = "fallback")
	public boolean processPayment(Long cartId, PaymentInfo paymentInfo) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()) {
			if (cart.get().isPaid()) {
				return true;
			}
			ProcessPaymentResponse response = paymentProxyClient.processPayment(new ProcessPaymentRequest(paymentInfo.getCardNumber(), paymentInfo.getExpMonth(), paymentInfo.getExpYear(), paymentInfo.getCvv(), cart.get().getTotalPrice()));
			cart.get().setPaid(response.isAccepted());
			return cart.get().isPaid();
		}
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public boolean fallback(Long cartId, PaymentInfo paymentInfo, Exception e) {
		logger.warning("Fallback method called from gateway");
		throw new CartNotFoundException("Payment service is not available: " + e.getMessage());
	}

		public boolean processDelivery(Long cartId, DeliveryInfo deliveryInfo) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent()) {
			if(cart.get().isPaid())
				return deliveryProxyClient.deliverOrder(new DeliverOrderRequest(deliveryInfo.getAddress(), deliveryInfo.getCity(), deliveryInfo.getNumber(), deliveryInfo.getZipcode(), deliveryInfo.getTelephoneNumber(), deliveryInfo.getScheduledTime(), cart.get().getRestaurantId(), cart.get().getId())).isAccepted();
			else return false;
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public Cart updateCartDetails(Cart cart) {
		 double totalPrice = 0;
		 for (CartItem item : cart.getItemList()) {
			 GetMenuItemDetailsResponse response = restaurantServiceClient.getMenuItemDetails(cart.getRestaurantId(), item.getId());
			 item.setPrice(response.getPrice());
			 item.setName(response.getName());
			 totalPrice += item.getPrice() * item.getQuantity();
			 if (item.getName()==null)
				 throw new MenuItemNotFoundException("Item with id " + item.getId() + " not found");
		 }
		 cart.setTotalPrice(totalPrice);
		 orderingRepository.save(cart);
		 return cart;
	}


//	@EventListener(RefreshScopeRefreshedEvent.class)
	@EventListener(RefreshScopeRefreshedEvent.class)
	public void refreshCircuitBreaker() {
		logger.info("RefreshListner: resetting cricuitbreaker");
		io.github.resilience4j.circuitbreaker.CircuitBreaker.State state = circuitBreaker.getState();
		circuitBreaker = circuitBreakerRegistry.circuitBreaker("orderingService");
		switch (state) {
			case OPEN -> circuitBreaker.transitionToOpenState();
			case HALF_OPEN -> circuitBreaker.transitionToHalfOpenState();
			default -> {
			}
		}
	}
}

