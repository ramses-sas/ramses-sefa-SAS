package polimi.saefa.orderingservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*; 

@Service
@Transactional
public class OrderingService {

	@Autowired
	private OrderingRepository orderingRepository;

	@Autowired
	private RestaurantServiceClient restaurantServiceClient;

	public Cart addItemToCart(Long cartId, Long restaurantId, String item, int quantity){
		 Cart cart = orderingRepository.findById(cartId).orElse(new Cart(restaurantId));

		 if(cart.addItem(item, restaurantId, quantity)) {
			 orderingRepository.save(cart);
		 }
		 return cart;
	}

	public Cart removeItemFromCart(Long cartId, Long restaurantId, String item, int quantity){
		Cart cart = orderingRepository.findById(cartId).orElse(new Cart(restaurantId));

		if(cart.removeItem(item, restaurantId, quantity)) {
			orderingRepository.save(cart);
		}
		return cart;
	}
	public boolean notifyRestaurant(Long cartId){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent()) {
			restaurantServiceClient.notifyRestaurant(cart.get().getRestaurantId(), cart.get().getId());
			return true;
		}
		return false;
	}

	public boolean processPayment(Long cartId, PaymentInfo paymentInfo){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent()) {

			return true;
		}
		return false;
	}

	public boolean processDelivery(Long cartId, DeliveryInfo deliveryInfo){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent()) {

			return true;
		}
		return false;
	}

	public double updateCartPrice(Cart cart){
		 double totalPrice = 0;
		 for (CartItem item : cart.getItemList()) {
			 totalPrice += restaurantServiceClient.getMenuItemPrice(cart.getRestaurantId(), item.getId()).getPrice() * item.getQuantity();
		 }
		 cart.setTotalPrice(totalPrice);
		 orderingRepository.save(cart);
		 return cart.getTotalPrice();
	}

	
}

