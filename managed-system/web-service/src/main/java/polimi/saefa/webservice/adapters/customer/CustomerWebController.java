package polimi.saefa.webservice.adapters.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import polimi.saefa.orderingservice.restapi.CartItemElementExtended;
import polimi.saefa.orderingservice.restapi.GetCartResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import polimi.saefa.webservice.domain.customer.CustomerWebService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Slf4j
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
	public String getRestaurant(HttpServletResponse response, Model model, @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long restaurantId) {
		// ["restaurantId:cartId"_"restaurantId:cartId"_ ...]
		CookieCartElement cartForRestaurant = getCookieCartElement(cartData, restaurantId);
		if (cartForRestaurant == null) {
			// TODO! - replace 11 with call to createCart()
			cartForRestaurant = new CookieCartElement(restaurantId.toString(), "11");
			appendToCookie(response, cartData, cartForRestaurant.getRestaurantId()+":"+cartForRestaurant.getCartId());
		}
		model.addAttribute("cartId", cartForRestaurant.getCartId());
		GetRestaurantResponse restaurant = customerWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "customer/get-restaurant";
	}

	/* Trova il menu del ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}/menu")
	public String getRestaurantMenu(HttpServletResponse response, Model model, @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long restaurantId) {
		// ["restaurantId:cartId"_"restaurantId:cartId"_ ...]
		CookieCartElement cartForRestaurant = getCookieCartElement(cartData, restaurantId);
		if (cartForRestaurant == null) {
			// TODO! - replace 11 with call to createCart()
			cartForRestaurant = new CookieCartElement(restaurantId.toString(), "11");
			appendToCookie(response, cartData, cartForRestaurant.getRestaurantId()+":"+cartForRestaurant.getCartId());
		}
		model.addAttribute("cartId", cartForRestaurant.getCartId());
		GetRestaurantResponse restaurant = customerWebService.getRestaurant(restaurantId);
		GetRestaurantMenuResponse menu = customerWebService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "customer/get-restaurant-menu";
	}

	/* Trova il carrello del ristorante con restaurantId dell'utente userId */
	@GetMapping("/cart/{cartId}")
	public String getCart(Model model, @PathVariable String cartId) {
		//GetCartResponse cart = customerWebService.getCart(cartId);
		//TODO - Replace with real logic
		ArrayList<CartItemElementExtended> aa = new ArrayList<>();
		aa.add(new CartItemElementExtended("1", "pizza", 10, 1));
		GetCartResponse cart = new GetCartResponse(Long.parseLong(cartId), 1L, 10.0, aa);
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
	public String confirmOrder(HttpServletResponse response, Model model, @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long cartId) {
		//TODO - Uncomment to use real logic
		/*CheckoutForm formData = (CheckoutForm) model.getAttribute("formData");
		customerWebService.confirmOrder(cartId, formData.getCardNumber(), formData.getExpMonth(), formData.getExpYear(),
				formData.getCvv(), formData.getAddress(), formData.getCity(), formData.getNumber(), formData.getZipcode(),
				formData.getTelephoneNumber(), formData.getScheduledTime());*/
		removeFromCookie(response, cartData, cartId.toString());
		return "customer/order-confirmed";
	}

	@Data
	@AllArgsConstructor
	private class CookieCartElement {
		public String restaurantId;
		public String cartId;
	}

	private CookieCartElement getCookieCartElement(String cookieValue, Long restaurantId) {
		String[] elements = cookieValue.split("_");
		for (String element : elements) {
			String[] components = element.split(":");
			if (components.length == 2) {
				String elemRestaurantId = components[0];
				String elemCartId = components[1];
				if (elemRestaurantId.equals(restaurantId.toString())) {
					return new CookieCartElement(elemRestaurantId, elemCartId);
				}
			}
		}
		return null;
	}
	private void appendToCookie(HttpServletResponse response, String initialCookie, String toAdd) {
		String updatedCookieValue = initialCookie.equals("") ? toAdd : initialCookie+"_"+toAdd;
		Cookie cookie = new Cookie("cartData", updatedCookieValue);
		cookie.setPath("/");
		cookie.setMaxAge(60 * 60 * 24 * 365);
		response.addCookie(cookie);
	}

	private void removeFromCookie(HttpServletResponse response, String initialCookie, String cartToRemove) {
		StringBuilder updatedCookieValue = new StringBuilder();
		for (String element : initialCookie.split("_")) {
			if (!element.contains(cartToRemove)) {
				updatedCookieValue.append(element).append("_");
			}
		}
		if (!updatedCookieValue.isEmpty() && updatedCookieValue.lastIndexOf("_") == updatedCookieValue.length()-1) {
			updatedCookieValue.deleteCharAt(updatedCookieValue.length()-1);
		}
		Cookie cookie = new Cookie("cartData", updatedCookieValue.toString());
		cookie.setPath("/");
		cookie.setMaxAge(60 * 60 * 24 * 365);
		response.addCookie(cookie);
	}

}
