package polimi.saefa.orderingservice.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import polimi.saefa.deliveryproxyservice.restapi.DeliverOrderRequest;
import polimi.saefa.orderingservice.exceptions.CartNotFoundException;
import polimi.saefa.orderingservice.exceptions.ItemRemovalException;
import polimi.saefa.orderingservice.exceptions.MenuItemNotFoundException;
import polimi.saefa.orderingservice.externalInterfaces.DeliveryProxyClient;
import polimi.saefa.orderingservice.externalInterfaces.PaymentProxyClient;
import polimi.saefa.orderingservice.externalInterfaces.RestaurantServiceClient;
import polimi.saefa.orderingservice.rest.OrderingRestController;
import polimi.saefa.paymentproxyservice.restapi.ProcessPaymentRequest;
import polimi.saefa.paymentproxyservice.restapi.ProcessPaymentResponse;
import polimi.saefa.restaurantservice.restapi.common.GetMenuItemDetailsResponse;
import polimi.saefa.restaurantservice.restapi.common.NotifyRestaurantResponse;

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

	private final Logger logger = Logger.getLogger(OrderingRestController.class.toString()); //TODO REMOVE

	public Cart getCart(Long cartId){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()) return cart.get();
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public Cart createCart(Long restaurantId) {
		Cart cart = new Cart(restaurantId);
		return orderingRepository.save(cart);
	}

	public Cart addItemToCart(Long cartId, Long restaurantId, String item, int quantity){
		 Cart cart = orderingRepository.findById(cartId).orElse(new Cart(restaurantId));

		 if (cart.addItem(item, restaurantId, quantity)) {
			 cart = updateCartDetails(cart);
		 }
		 return cart;
	}

	public Cart removeItemFromCart(Long cartId, Long restaurantId, String item, int quantity){
		Optional<Cart> cart = orderingRepository.findById(cartId);

		if (cart.isPresent())
			if(cart.get().removeItem(item, restaurantId, quantity))
				return updateCartDetails(cart.get());
			else throw new ItemRemovalException("Impossible to remove the selected item from cart " + cartId);
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}
	public boolean notifyRestaurant(Long cartId){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent() && cart.get().isPaid()) {
			NotifyRestaurantResponse response = restaurantServiceClient.notifyRestaurant(cart.get().getRestaurantId(), cart.get().getId());
			return response.isNotified();
			//TODO WHAT TO DO WITH THE RESPONSE?
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public boolean processPayment(Long cartId, PaymentInfo paymentInfo) {
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if (cart.isPresent()) {
			ProcessPaymentResponse response = paymentProxyClient.processPayment(new ProcessPaymentRequest(paymentInfo.getCardNumber(), paymentInfo.getExpMonth(), paymentInfo.getExpYear(), paymentInfo.getCvv(), cart.get().getTotalPrice()));
			logger.info("Processing payment, accepted: " + response.isAccepted()); //TODO REMOVE
			cart.get().setPaid(response.isAccepted());
			return cart.get().isPaid();
			//TODO AGGIUNGERE CONTROLLO CARRELLO GIÀ PAGATO? SECONDO ME NO, TROPPO SBATTI CREARE NUOVA ECCEZIONE
		}
		else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public boolean processDelivery(Long cartId, DeliveryInfo deliveryInfo){
		Optional<Cart> cart = orderingRepository.findById(cartId);
		if(cart.isPresent()) {
			if(cart.get().isPaid())
				return deliveryProxyClient.deliverOrder(new DeliverOrderRequest(deliveryInfo.getAddress(), deliveryInfo.getCity(), deliveryInfo.getNumber(), deliveryInfo.getZipcode(), deliveryInfo.getTelephoneNumber(), deliveryInfo.getScheduledTime(), cart.get().getRestaurantId(), cart.get().getId())).isAccepted();
			else return false;
		} else throw new CartNotFoundException("Cart with id " + cartId + " not found");
	}

	public Cart updateCartDetails(Cart cart){
		 double totalPrice = 0;
		 for (CartItem item : cart.getItemList()) {
			 GetMenuItemDetailsResponse response = restaurantServiceClient.getMenuItemDetails(cart.getRestaurantId(), item.getId());
			 item.setPrice(response.getPrice());
			 item.setName(response.getName());
			 totalPrice += item.getPrice() * item.getQuantity();
			 if(item.getName()==null)
				 throw new MenuItemNotFoundException("Item with id " + item.getId() + " not found");
		 }
		 cart.setTotalPrice(totalPrice);
		 orderingRepository.save(cart);
		 return cart;
	}

	
}

