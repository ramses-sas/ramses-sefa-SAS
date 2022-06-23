package polimi.saefa.restaurantservice.adapters.client.rest;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;

import polimi.saefa.restaurantservice.domain.*;
import polimi.saefa.restaurantservice.restapi.common.*;

import java.util.*; 
import java.util.stream.*;
import java.util.logging.Logger;

@RestController
@RequestMapping(path="/client/rest")
public class ClientRestController {

	@Autowired 
	private RestaurantService restaurantService;
	
    private final Logger logger = Logger.getLogger(ClientRestController.class.toString());


	/* Trova il ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}")
	public GetRestaurantResponse getRestaurant(@PathVariable Long restaurantId) {
		logger.info("REST CALL: getRestaurant " + restaurantId); 
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		return restaurantToGetRestaurantResponse(restaurant);
	}
	
	/* Trova tutti i ristoranti. */ 
	@GetMapping("/restaurants")
	public GetRestaurantsResponse getRestaurants() {
		logger.info("REST CALL: getAllRestaurants"); 
		Collection<Restaurant> restaurants = restaurantService.getAllRestaurants();
		Collection<GetRestaurantResponse> restaurantResponses = 
			restaurants
				.stream()
				.map(this::restaurantToGetRestaurantResponse)
				.collect(Collectors.toList());
		return new GetRestaurantsResponse(restaurantResponses);
	}
	
	/* Trova il menu del ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}/menu")
	public GetRestaurantMenuResponse getRestaurantMenu(@PathVariable Long restaurantId) {
		logger.info("REST CALL: getRestaurantMenu " + restaurantId); 
		RestaurantMenu menu = restaurantService.getRestaurantMenu(restaurantId);
		List<MenuItemElement> menuItemElements = 
			menu.getMenuItems() 
				.stream()
				.map(this::menuItemToMenuItemElement)
				.collect(Collectors.toList());
		GetRestaurantMenuResponse response = 
			new GetRestaurantMenuResponse(restaurantId, menuItemElements);
		return response; 
	}

	private MenuItemElement menuItemToMenuItemElement(MenuItem item) {
		return new MenuItemElement(item.getId(), item.getName(), item.getPrice());
	}

	private GetRestaurantResponse restaurantToGetRestaurantResponse(Restaurant restaurant) {
		if (restaurant != null)
			return new GetRestaurantResponse(restaurant.getId(), restaurant.getName(), restaurant.getLocation());
		return null;
	}

}
