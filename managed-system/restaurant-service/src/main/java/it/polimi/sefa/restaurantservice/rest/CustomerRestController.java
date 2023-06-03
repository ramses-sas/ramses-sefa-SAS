package it.polimi.sefa.restaurantservice.rest;

import it.polimi.sefa.restaurantservice.domain.MenuItem;
import it.polimi.sefa.restaurantservice.domain.Restaurant;
import it.polimi.sefa.restaurantservice.domain.RestaurantMenu;
import it.polimi.sefa.restaurantservice.domain.RestaurantService;
import it.polimi.sefa.restaurantservice.restapi.common.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*; 
import java.util.stream.*;

@Slf4j
@RestController
@RequestMapping(path="/rest/customer")
public class CustomerRestController {
	@Autowired 
	private RestaurantService restaurantService;

	@GetMapping("/restaurants/{restaurantId}")
	public GetRestaurantResponse getRestaurant(@PathVariable Long restaurantId) {
		log.info("REST CALL: getRestaurant " + restaurantId);
		Restaurant restaurant = restaurantService.getRestaurant(restaurantId);
		return restaurantToGetRestaurantResponse(restaurant);
	}
	
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

	// Dummy method to simulate the restaurant notification functionality
	@PostMapping("/restaurants/{restaurantId}/notify")
	public NotifyRestaurantResponse notifyRestaurant(@PathVariable Long restaurantId, @RequestBody NotifyRestaurantRequest request) {
		log.info("REST CALL: notifyRestaurant " + restaurantId + " order " + request.getOrderNumber());
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
