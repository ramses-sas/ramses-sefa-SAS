package polimi.saefa.orderingservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*; 

@Service
@Transactional
public class OrderingService {

//	@Autowired
//	private RestaurantRepository restaurantRepository;

 	public String dummyMethod(String myString) {
		return myString;
	}

	public boolean addItemToCart(Cart cart, String restaurantId, String item, int quantity){
		 return cart.addItem(item, restaurantId, quantity);
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
		 for(CartItem item:cart.getItems()){
			 //chiama il rest service e chiedi il prezzo dell'item
			 double itemPrice = 0;
			 totalPrice+=item.getQuantity()*itemPrice;
		 }
		 cart.setTotalPrice(totalPrice);
		 return true;
	}

	
}

