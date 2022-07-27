package polimi.saefa.webservice.adapters.customer;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import polimi.saefa.orderingservice.restapi.CartItemElementExtended;
import polimi.saefa.orderingservice.restapi.ConfirmOrderRequest;
import polimi.saefa.orderingservice.restapi.ConfirmOrderResponse;
import polimi.saefa.orderingservice.restapi.GetCartResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import polimi.saefa.webservice.domain.customer.CustomerWebService;

import java.util.*;

@Controller
@RequestMapping(path="/customer")
public class CustomerWebController {

	@Autowired 
	private CustomerWebService customerWebService;

	/* Mostra client home page */
	@GetMapping("")
	public String index() {
		return "customer/index";
	}

	/* Trova tutti i ristoranti. */
	@GetMapping("/restaurants")
	public String getRestaurants(Model model) {
		Collection<GetRestaurantResponse> restaurants = customerWebService.getAllRestaurants();
		model.addAttribute("restaurants", restaurants);
		return "customer/get-restaurants";
	}


	/* Trova il ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}")
	public String getRestaurant(Model model, @PathVariable Long restaurantId) {
		GetRestaurantResponse restaurant = customerWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "customer/get-restaurant";
	}

	/* Trova il menu del ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}/menu")
	public String getRestaurantMenu(Model model, @PathVariable Long restaurantId) {
		GetRestaurantResponse restaurant = customerWebService.getRestaurant(restaurantId);
		GetRestaurantMenuResponse menu = customerWebService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "customer/get-restaurant-menu";
	}

	/* Trova il carrello del ristorante con restaurantId dell'utente userId */
	@GetMapping("/cart/{cartId}")
	public String getCart(Model model, @PathVariable Long cartId) {
		//GetCartResponse cart = customerWebService.getCart(cartId);
		//TODO - Replace with real logic
		ArrayList<CartItemElementExtended> aa = new ArrayList<>();
		aa.add(new CartItemElementExtended("1", "pizza", 10, 1));
		GetCartResponse cart = new GetCartResponse(1L, 1L, 10.0, aa);

		model.addAttribute("cart", cart);
		return "customer/cart";
	}

	@GetMapping("/cart/{cartId}/checkout")
	public String redirectToCheckoutForm(Model model, @PathVariable Long cartId) {
		CheckoutForm formData = new CheckoutForm();
		model.addAttribute("cartId", cartId);
		model.addAttribute("formData", formData);
		return "customer/checkout-form";
	}

	@PostMapping("/cart/{cartId}/confirmOrder")
	public String confirmOrder(Model model, @PathVariable Long cartId) {
		//TODO - Uncomment to use real logic
		/*CheckoutForm formData = (CheckoutForm) model.getAttribute("formData");
		customerWebService.confirmOrder(cartId, formData.getCardNumber(), formData.getExpMonth(), formData.getExpYear(),
				formData.getCvv(), formData.getAddress(), formData.getCity(), formData.getNumber(), formData.getZipcode(),
				formData.getTelephoneNumber(), formData.getScheduledTime());*/
		return "customer/order-confirmed";
	}

}
