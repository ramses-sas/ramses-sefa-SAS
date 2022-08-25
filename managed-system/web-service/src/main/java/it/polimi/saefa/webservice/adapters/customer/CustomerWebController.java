package it.polimi.saefa.webservice.adapters.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import it.polimi.saefa.orderingservice.restapi.*;
import it.polimi.saefa.restaurantservice.restapi.common.*;
import it.polimi.saefa.webservice.domain.customer.CustomerWebService;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;

@Controller
@Slf4j
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
			CreateCartResponse cartResponse = customerWebService.createCart(restaurantId);
			cartForRestaurant = new CookieCartElement(restaurantId, cartResponse.getId());
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
			CreateCartResponse cartResponse = customerWebService.createCart(restaurantId);
			cartForRestaurant = new CookieCartElement(restaurantId, cartResponse.getId());
			appendToCookie(response, cartData, cartForRestaurant.getRestaurantId()+":"+cartForRestaurant.getCartId());
		}
		model.addAttribute("cartId", cartForRestaurant.getCartId());
		GetRestaurantResponse restaurant = customerWebService.getRestaurant(restaurantId);
		GetRestaurantMenuResponse menu = customerWebService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "customer/get-restaurant-menu";
	}

	/* Trova il carrello con id=cartId */
	@GetMapping("/cart/{cartId}")
	public String getCart(Model model, @PathVariable Long cartId) {
		GetCartResponse cart = customerWebService.getCart(cartId);
		model.addAttribute("cart", cart);
		return "customer/cart";
	}

	/* Trova il carrello con id=cartId */
	@PostMapping("/cart/{cartId}/addItem")
	public String addItemToCart(Model model, @PathVariable Long cartId, @RequestParam Long restaurantId, @RequestParam String itemId) {
		AddItemToCartResponse cart = customerWebService.addItemToCart(cartId, restaurantId, itemId, 1);
		model.addAttribute("cart", cart);
		return "customer/cart";
	}

	@GetMapping("/cart/{cartId}/checkout")
	public String redirectToCheckoutForm(Model model, @PathVariable Long cartId) {
		CheckoutForm formData = new CheckoutForm(
				"1234123412341234","12","2111","012","Via Roma","Roma","6","00012","1234567890","2100-12-12T12:12:12Z");
		model.addAttribute("cartId", cartId);
		model.addAttribute("formData", formData);
		return "customer/checkout-form";
	}

	@PostMapping("/cart/{cartId}/confirmOrder")
	public String confirmOrder(HttpServletResponse response, Model model, @ModelAttribute("formData") CheckoutForm formData,
							   @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long cartId) {
		Date d;
		int expMonth, expYear, streetNumber;
		Consumer<String> handleError = (errString) -> {
			model.addAttribute("cartId", cartId);
			model.addAttribute("formData", formData);
			model.addAttribute("error", errString);
		};

		try {
			LocalDateTime ld = LocalDateTime.parse(formData.getScheduledTime());
			d = Date.from(ld.toInstant(ZoneOffset.UTC));
			expMonth = Integer.parseInt(formData.getExpMonth());
			expYear = Integer.parseInt(formData.getExpYear());
			streetNumber = Integer.parseInt(formData.getNumber());
		} catch (DateTimeParseException e) {
			log.error("Invalid scheduled time. " + e.getMessage());
			handleError.accept("Invalid scheduled time");
			return "customer/checkout-form";
		} catch (NumberFormatException e) {
			log.error("Invalid integers. " + e.getMessage());
			handleError.accept("Invalid values for ExpMonth, ExpYear or Street Number. They must be integers.");
			return "customer/checkout-form";
		}
		ConfirmOrderResponse confirmOrderResponse = customerWebService.confirmOrder(cartId, formData.getCardNumber(), expMonth,
				expYear, formData.getCvv(), formData.getAddress(), formData.getCity(),
				streetNumber, formData.getZipcode(), formData.getTelephoneNumber(), d);
		model.addAttribute("confirmOrderResponse", confirmOrderResponse);
		if(!confirmOrderResponse.isConfirmed()){
			if(confirmOrderResponse.getIsTakeAway() != null && confirmOrderResponse.getIsTakeAway()){
				return "customer/takeaway-confirmation";
			}
			else if (confirmOrderResponse.getRequiresCashPayment() != null && confirmOrderResponse.getRequiresCashPayment()){
				model.addAttribute("formData", formData);
				return "customer/cash-payment";

			}
			else{
				handleError.accept("Order not confirmed");
			}

		}
		removeFromCookie(response, cartData, cartId.toString());
		return "customer/order-confirmed";
	}


	@PostMapping("/cart/{cartId}/confirmCashPayment")
	public String confirmCashPayment(HttpServletResponse response, Model model, @ModelAttribute("formData") CheckoutForm formData,
							   @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long cartId) {
		Date d;
		int streetNumber;
		Consumer<String> handleError = (errString) -> {
			model.addAttribute("cartId", cartId);
			model.addAttribute("formData", formData);
			model.addAttribute("error", errString);
		};

		try {
			LocalDateTime ld = LocalDateTime.parse(formData.getScheduledTime());
			d = Date.from(ld.toInstant(ZoneOffset.UTC));
			streetNumber = Integer.parseInt(formData.getNumber());
		} catch (DateTimeParseException e) {
			log.error("Invalid scheduled time. " + e.getMessage());
			handleError.accept("Invalid scheduled time");
			return "customer/cash-payment";
		} catch (NumberFormatException e) {
			log.error("Invalid integers. " + e.getMessage());
			handleError.accept("Invalid values for ExpMonth, ExpYear or Street Number. They must be integers.");
			return "customer/cash-payment";
		}
		ConfirmOrderResponse confirmOrderResponse = customerWebService.confirmCashPayment(cartId, formData.getAddress(), formData.getCity(),
				streetNumber, formData.getZipcode(), formData.getTelephoneNumber(), d);
		model.addAttribute("confirmOrderResponse", confirmOrderResponse);
		if(!confirmOrderResponse.isConfirmed()) {
			if (confirmOrderResponse.getIsTakeAway() != null && confirmOrderResponse.getIsTakeAway()) {
				return "customer/takeaway-confirmation";
			} else {
				handleError.accept("Order not confirmed");
			}
		}
		removeFromCookie(response, cartData, cartId.toString());
		model.addAttribute("cash", "You will be asked to pay cash upon delivery.");

		return "customer/order-confirmed";
	}

	@PostMapping("/cart/{cartId}/confirmTakeAway")
	public String confirmTakeAway(HttpServletResponse response, Model model, @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long cartId) {
		ConfirmOrderResponse confirmOrderResponse = customerWebService.handleTakeAway(cartId, true);
		if(!confirmOrderResponse.isConfirmed()){
			model.addAttribute("cartId", cartId);
			model.addAttribute("error", "Order not confirmed");
		}
		else{
			model.addAttribute("takeaway", "You can pick up the order at the restaurant at the specified time.");
			if (confirmOrderResponse.getRequiresCashPayment()!= null && confirmOrderResponse.getRequiresCashPayment())
				model.addAttribute("cash", "You will be asked to pay cash at the restaurant.");
		}
		removeFromCookie(response, cartData, cartId.toString());
		return "customer/order-confirmed";
	}

	@PostMapping("/cart/{cartId}/rejectTakeAway")
	public String rejectTakeAway(HttpServletResponse response, Model model, @CookieValue(value = "cartData", defaultValue = "") String cartData, @PathVariable Long cartId) {
		customerWebService.handleTakeAway(cartId, false);
		removeFromCookie(response, cartData, cartId.toString());
		return "customer/order-canceled";
	}




	@Data
	@AllArgsConstructor
	private class CookieCartElement {
		public Long restaurantId;
		public Long cartId;
	}

	private CookieCartElement getCookieCartElement(String cookieValue, Long restaurantId) {
		String[] elements = cookieValue.split("_");
		for (String element : elements) {
			String[] components = element.split(":");
			if (components.length == 2) {
				Long elemRestaurantId = Long.parseLong(components[0]);
				Long elemCartId = Long.parseLong(components[1]);
				if (elemRestaurantId.equals(restaurantId)) {
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
