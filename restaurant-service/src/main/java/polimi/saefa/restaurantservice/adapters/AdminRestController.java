package polimi.saefa.restaurantservice.adapters;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import polimi.saefa.restaurantservice.domain.*;
import polimi.saefa.restaurantservice.restapi.admin.*;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantsResponse;
import polimi.saefa.restaurantservice.restapi.common.MenuItemElement;

import java.util.*; 
import java.util.stream.*;
import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/admin")
public class AdminRestController {

	@Autowired 
	private RestaurantService restaurantService;
	
    private final Logger logger = Logger.getLogger(AdminRestController.class.toString());

	/* Crea un nuovo ristorante. */ 
	@PostMapping("/restaurants")
	public CreateRestaurantResponse createRestaurant(@RequestBody CreateRestaurantRequest request) {
		String name = request.getName();
		String location = request.getLocation();
		logger.info("REST CALL: createRestaurant " + name + ", " + location); 
		Restaurant restaurant = restaurantService.createRestaurant(name, location);
		return new CreateRestaurantResponse(restaurant.getId(), restaurant.getName(), restaurant.getLocation());
	}	

	/* Crea o modifica il menu del del ristorante con restaurantId. */ 
	@PutMapping("/restaurants/{restaurantId}/menu")
	public CreateRestaurantMenuResponse createRestaurantMenu(@RequestBody CreateRestaurantMenuRequest request) {
		logger.warning("REST CALL: starting createRestaurantMenu ");
		logger.warning(request.toString());
		Long restaurantId = request.getRestaurantId(); 
		List<MenuItem> menuItems =
			request.getMenuItems() 
				.stream()
				.map(this::menuItemElementToMenuItem)
				.collect(Collectors.toList());
		logger.info("REST CALL: createRestaurantMenu " + restaurantId + ", " + menuItems); 
		Restaurant restaurant = restaurantService.createOrUpdateRestaurantMenu(restaurantId, menuItems);
		return new CreateRestaurantMenuResponse(restaurant.getId());
	}

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


	private GetRestaurantResponse restaurantToGetRestaurantResponse(Restaurant restaurant) {
		if (restaurant != null) {
			return new GetRestaurantResponse(restaurant.getId(), restaurant.getName(), restaurant.getLocation());
		} else {
			return null;
		}
	}

	private MenuItemElement menuItemToMenuItemElement(MenuItem item) {
		return new MenuItemElement(item.getId(), item.getName(), item.getPrice());
	}

	private MenuItem menuItemElementToMenuItem(MenuItemElement item) {
		return new MenuItem(item.getId(), item.getName(), item.getPrice());
	}

}
