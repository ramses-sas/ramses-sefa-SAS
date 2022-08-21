package polimi.saefa.orderingservice.domain;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimi.saefa.orderingservice.exceptions.*;
import polimi.saefa.orderingservice.externalInterfaces.*;
import polimi.saefa.orderingservice.rest.OrderingRestController;
import polimi.saefa.paymentproxyservice.restapi.*;
import polimi.saefa.deliveryproxyservice.restapi.*;
import polimi.saefa.restaurantservice.restapi.common.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import java.util.Optional;
import java.util.logging.Logger;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;

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
	//public io.github.resilience4j.circuitbreaker.CircuitBreaker paymentCircuitBreaker;

	//public io.github.resilience4j.circuitbreaker.CircuitBreaker deliveryCircuitBreaker;


	private final Logger logger = Logger.getLogger(OrderingRestController.class.toString());

	public OrderingService(CircuitBreakerRegistry circuitBreakerRegistry) {
		this.circuitBreakerRegistry = circuitBreakerRegistry;
		//paymentCircuitBreaker = circuitBreakerRegistry.circuitBreaker("payment", "payment");
		//deliveryCircuitBreaker = circuitBreakerRegistry.circuitBreaker("delivery", "delivery");
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
	public boolean notifyRestaurant(Long cartId, boolean isTakeaway) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent() && (cart.get().isPaid()|| cart.get().isRequiresCashPayment())) {
			cart.get().setRequiresTakeaway(isTakeaway);
			NotifyRestaurantResponse response = restaurantServiceClient.notifyRestaurant(cart.get().getRestaurantId(), new NotifyRestaurantRequest(cart.get().getId(), isTakeaway, cart.get().isRequiresCashPayment(), cart.get().getTotalPrice()));
			return response.isNotified();
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	@CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
	public boolean processPayment(Long cartId, PaymentInfo paymentInfo) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()) {
			if (cart.get().isPaid() || cart.get().isRequiresCashPayment()) {
				return true;
			}
			ProcessPaymentResponse response = paymentProxyClient.processPayment(new ProcessPaymentRequest(paymentInfo.getCardNumber(), paymentInfo.getExpMonth(), paymentInfo.getExpYear(), paymentInfo.getCvv(), cart.get().getTotalPrice()));
			cart.get().setPaid(response.isAccepted());
			return cart.get().isPaid();
		}
		//else return true; TODO REMOVE THIS ELSE AND RESTORE THE NEXT. RESTORE AND COMMENT THE ONE BELOW TO QUICKLY TEST DELIVERY CB
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public boolean paymentFallback(Long cartId, PaymentInfo paymentInfo, RuntimeException e) {
		logger.warning("Payment fallback method called from gateway");

		if(circuitBreakerRegistry.circuitBreaker("payment").getCircuitBreakerConfig().getIgnoreExceptionPredicate().test(e))
			throw e;
		//if(paymentCircuitBreaker.getCircuitBreakerConfig().getIgnoreExceptionPredicate().test(e) && e instanceof RuntimeException)
		//	throw (RuntimeException)e;
		throw new PaymentNotAvailableException("Payment service is not available: " + e.getMessage(), cartId);
	}

	public boolean confirmCashPayment(Long cartId) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()) {
			cart.get().setRequiresCashPayment(true);
			return cart.get().isRequiresCashPayment();
		}
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}
	@CircuitBreaker(name = "delivery", fallbackMethod = "deliveryFallback")
		public boolean processDelivery(Long cartId, DeliveryInfo deliveryInfo) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent()) {
			if(cart.get().isPaid() || cart.get().isRequiresCashPayment())
				return deliveryProxyClient.deliverOrder(new DeliverOrderRequest(deliveryInfo.getAddress(), deliveryInfo.getCity(), deliveryInfo.getNumber(), deliveryInfo.getZipcode(), deliveryInfo.getTelephoneNumber(), deliveryInfo.getScheduledTime(), cart.get().getRestaurantId(), cart.get().getId(), cart.get().getTotalPrice())).isAccepted();
			else return false;
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}


	public boolean deliveryFallback(Long cartId, DeliveryInfo deliveryInfo, RuntimeException e) {
		logger.warning("Delivery fallback method called from gateway");
		if(circuitBreakerRegistry.circuitBreaker("delivery").getCircuitBreakerConfig().getIgnoreExceptionPredicate().test(e))
			throw e;
		//if(deliveryCircuitBreaker.getCircuitBreakerConfig().getIgnoreExceptionPredicate().test(e) && e instanceof RuntimeException)
		//	throw (RuntimeException)e;
		throw new DeliveryNotAvailableException("Delivery service is not available: " + e.getMessage(), cartId);
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

	public boolean orderRequiresCashPayment(Long cartId){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()){
			return cart.get().isRequiresCashPayment();
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public boolean orderRequiresTakeaway(Long cartId){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()){
			return cart.get().isRequiresTakeaway();
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	/*
	@EventListener(RefreshScopeRefreshedEvent.class)
	public void refreshCircuitBreaker() {
		logger.info("Configuration changed, resetting cricuitbreakers");
		State paymentState = paymentCircuitBreaker.getState();
		State deliveryState = deliveryCircuitBreaker.getState();
		//logger.warning(System.identityHashCode(paymentCircuitBreaker) + " " + System.identityHashCode(deliveryCircuitBreaker));
		paymentCircuitBreaker = circuitBreakerRegistry.circuitBreaker("payment", "payment");
		deliveryCircuitBreaker = circuitBreakerRegistry.circuitBreaker("delivery", "delivery");
		//logger.warning("Dopo:" + System.identityHashCode(paymentCircuitBreaker) + " " + System.identityHashCode(deliveryCircuitBreaker));
		switch (paymentState) {
			case OPEN -> paymentCircuitBreaker.transitionToOpenState();
			case HALF_OPEN -> paymentCircuitBreaker.transitionToHalfOpenState();
			default -> {
			}
		}
		switch (deliveryState) {
			case OPEN -> deliveryCircuitBreaker.transitionToOpenState();
			case HALF_OPEN -> deliveryCircuitBreaker.transitionToHalfOpenState();
			default -> {
			}
		}
	}
	*/
}

