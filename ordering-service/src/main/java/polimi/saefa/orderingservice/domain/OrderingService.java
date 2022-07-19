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

 	public String dummyMethod(String myString) {
		return myString;
	}

	public Cart addItemToCart(Long cartId, String restaurantId, String item, int quantity){
		 Cart cart = orderingRepository.findById(cartId).orElse(new Cart(restaurantId));

		 if(cart.addItem(item, restaurantId, quantity)) {
			 orderingRepository.save(cart);
		 }
		 return cart;
	}

	public Cart removeItemFromCart(Long cartId, String restaurantId, String item, int quantity){
		Cart cart = orderingRepository.findById(cartId).orElse(new Cart(restaurantId));

		if(cart.removeItem(item, restaurantId, quantity)) {
			orderingRepository.save(cart);
		}
		return cart;
	}
	public boolean notifyRestaurant(Cart cart){
		 //method to invoke the restaurantService API in order to notify it about a completed order
		return true;
	}

	public boolean processPayment(Cart cart, PaymentInfo paymentInfo){
		 //contact the payment proxy
		 return true;
	}

	public boolean processDelivery(Cart cart, DeliveryInfo deliveryInfo){
		//contact the delivery proxy
		return true;
	}

	public boolean updateCartPrice(Cart cart){
		 double totalPrice = 0;
		 for(CartItem item:cart.getItemList()){
			 //chiama il rest service e chiedi il prezzo dell'item
			 double itemPrice = 0;
			 totalPrice+=item.getQuantity()*itemPrice;
		 }
		 cart.setTotalPrice(totalPrice);
		 orderingRepository.save(cart);
		 return true;
	}

	
}

