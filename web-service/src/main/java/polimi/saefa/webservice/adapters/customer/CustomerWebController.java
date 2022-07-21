package polimi.saefa.webservice.adapters.customer;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import polimi.saefa.webservice.domain.admin.AdminWebService;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping(path="/customer")
public class CustomerWebController {

	@Autowired 
	private AdminWebService adminWebService;

	/* Mostra client home page */
	@GetMapping("")
	public String index() {
		return "customer/index";
	}

	/* Trova tutti i ristoranti. */
	@GetMapping("/restaurants")
	public String getRestaurants(Model model) throws IOException {
		Collection<GetRestaurantResponse> restaurants = adminWebService.getAllRestaurants();
		model.addAttribute("restaurants", restaurants);
		return "customer/get-restaurants";
	}


	/* Trova il ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}")
	public String getRestaurant(Model model, @PathVariable Long restaurantId) throws IOException {
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "customer/get-restaurant";
	}

	/* Trova il menu del ristorante con restaurantId. */
	@GetMapping("/restaurants/{restaurantId}/menu")
	public String getRestaurantMenu(Model model, @PathVariable Long restaurantId) throws IOException {
		GetRestaurantResponse restaurant = adminWebService.getRestaurant(restaurantId);
		GetRestaurantMenuResponse menu = adminWebService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "customer/get-restaurant-menu";
	}

}
