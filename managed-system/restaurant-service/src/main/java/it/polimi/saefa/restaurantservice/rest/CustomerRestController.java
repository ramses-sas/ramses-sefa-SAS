package it.polimi.saefa.restaurantservice.rest;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import it.polimi.saefa.restaurantservice.domain.*;
import it.polimi.saefa.restaurantservice.restapi.common.*;
import java.util.*; 
import java.util.stream.*;
import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/customer")
public class CustomerRestController {

	@Autowired 
	private RestaurantService restaurantService;

    private final Logger logger = Logger.getLogger(CustomerRestController.class.toString());


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
		return new GetRestaurantMenuResponse(restaurantId, menuItemElements);
	}

	/* Ottiene il prezzo del prodotto specificato dal menù del ristorante selezionato. */
	@GetMapping("/restaurants/{restaurantId}/item/{itemId}")
	public GetMenuItemDetailsResponse getMenuItemDetails(@PathVariable Long restaurantId, @PathVariable String itemId) {
		logger.info("REST CALL: getMenuItemPrice restaurant: " + restaurantId + " item: " + itemId);
		RestaurantMenu menu = restaurantService.getRestaurantMenu(restaurantId);
		for (MenuItem item:menu.getMenuItems()){
			if(item.getId().equals(itemId))
				return new GetMenuItemDetailsResponse(item.getId(), item.getName(), item.getPrice());
		}
		return new GetMenuItemDetailsResponse();
	}

	@GetMapping("/restaurants/{restaurantId}/notify/{orderNumber}")
	public NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @PathVariable Long orderNumber) {
		logger.info("REST CALL: notifyRestaurant " + restaurantId + " order " + orderNumber);
		return new NotifyRestaurantResponse(true);
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