package it.polimi.sofa.restaurantservice.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import it.polimi.sofa.restaurantservice.domain.*;
import it.polimi.sofa.restaurantservice.restapi.common.*;

import java.util.*; 
import java.util.stream.*;
import java.util.logging.Logger;

@Slf4j
@RestController
@RequestMapping(path="/rest/customer")
public class CustomerRestController {
	@Autowired 
	private RestaurantService restaurantService;

	/* Trova il ristorante con restaurantId. */ 
	@GetMapping("/restaurants/{restaurantId}")
	public GetRestaurantResponse getRestaurant(@PathVariable Long restaurantId) {
		log.info("REST CALL: getRestaurant " + restaurantId);
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		return restaurantToGetRestaurantResponse(restaurant);
	}
	
	/* Trova tutti i ristoranti. */ 
	@GetMapping("/restaurants")
	public GetRestaurantsResponse getRestaurants() {
		log.info("REST CALL: getAllRestaurants");
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
		log.info("REST CALL: getRestaurantMenu " + restaurantId);
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
		log.info("REST CALL: getMenuItemPrice restaurant: " + restaurantId + " item: " + itemId);
		RestaurantMenu menu = restaurantService.getRestaurantMenu(restaurantId);
		for (MenuItem item: menu.getMenuItems()) {
			if (item.getId().equals(itemId))
				return new GetMenuItemDetailsResponse(item.getId(), item.getName(), item.getPrice());
		}
		return new GetMenuItemDetailsResponse();
	}

	@PostMapping("/restaurants/{restaurantId}/notify")
	public NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @RequestBody NotifyRestaurantRequest request) {
		log.info("REST CALL: notifyRestaurant " + restaurantId + " order " + request.getOrderNumber());
		//Dumb method to simulate restaurant functionality
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
