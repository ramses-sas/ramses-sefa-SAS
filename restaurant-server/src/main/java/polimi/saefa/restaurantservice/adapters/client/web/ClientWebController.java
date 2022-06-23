package polimi.saefa.restaurantservice.adapters.client.web;

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;
import polimi.saefa.restaurantservice.domain.*;

import java.util.*;
import java.util.logging.Logger;

@Controller
@RequestMapping(path="/client/web")
public class ClientWebController {
	private final Logger logger = Logger.getLogger(ClientWebController.class.toString());

	@Autowired 
	private RestaurantService restaurantService;

	/* Mostra client home page */
	@GetMapping("")
	public String index() {
		return "client/index";
	}

	/* Trova il ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}")
	public String getRestaurant(Model model, @PathVariable Long restaurantId) {
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		model.addAttribute("restaurant", restaurant);
		return "client/get-restaurant";
	}

	/* Trova il menu del ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}/menu")
	public String getRestaurantMenu(Model model, @PathVariable Long restaurantId) {
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		RestaurantMenu menu = restaurantService.getRestaurantMenu(restaurantId);
		model.addAttribute("restaurant", restaurant);
		model.addAttribute("menu", menu);
		return "client/get-restaurant-menu";
	}

	/* Trova tutti i ristoranti. */ 
	@GetMapping("/restaurants")
	public String getRestaurants(Model model) {
		Collection<Restaurant> restaurants = restaurantService.getAllRestaurants();
		model.addAttribute("restaurants", restaurants);
		return "client/get-restaurants";
	}

}
