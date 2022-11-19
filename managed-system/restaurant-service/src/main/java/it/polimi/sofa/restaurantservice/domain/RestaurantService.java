package it.polimi.sofa.restaurantservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import it.polimi.sofa.restaurantservice.exceptions.RestaurantNotFoundException;

import java.util.*; 

@Service
@Transactional
public class RestaurantService {
	@Autowired
	private RestaurantRepository restaurantRepository;

 	public Restaurant createRestaurant(String name, String location) {
		Restaurant restaurant = new Restaurant(name, location); 
		restaurant = restaurantRepository.save(restaurant);
		return restaurant;
	}

 	public Restaurant createRestaurantWithMenu(String name, String location, List<MenuItem> menuItems) {
		RestaurantMenu menu = new RestaurantMenu(menuItems);
		Restaurant restaurant = new Restaurant(name, location, menu); 
		restaurant = restaurantRepository.save(restaurant);
		return restaurant;
	}

 	public Restaurant createOrUpdateRestaurantMenu(Long id, List<MenuItem> menuItems) {
		Restaurant restaurant = restaurantRepository.findById(id).orElse(null);
		if (restaurant==null)
			throw new RestaurantNotFoundException("Restaurant with id " + id + "not found");
		RestaurantMenu menu = new RestaurantMenu(menuItems);
		restaurant.setMenu(menu); 
		restaurant = restaurantRepository.save(restaurant);
		return restaurant;
	}
	
 	public Restaurant getRestaurant(Long id) {
		 Restaurant restaurant = restaurantRepository.findById(id).orElse(null);
		 if (restaurant == null)
			 throw new RestaurantNotFoundException("Restaurant with id " + id + "not found");
		 else return restaurant;
	}

 	public RestaurantMenu getRestaurantMenu(Long id) {
		Restaurant restaurant = restaurantRepository.findByIdWithMenu(id);
		if (restaurant == null)
			throw new RestaurantNotFoundException("Restaurant with id " + id + "not found");
		else return restaurant.getMenu();
	}

 	public Restaurant getRestaurantByName(String name) {
		Restaurant restaurant = restaurantRepository.findByName(name);
		if (restaurant == null)
			throw new RestaurantNotFoundException("Restaurant with name " + name + "not found");
		else return restaurant;
	}
	
	public Collection<Restaurant> getAllRestaurants() {
		return restaurantRepository.findAll();
	}
	
	public Collection<Restaurant> getAllRestaurantsByLocation(String location) {
		return restaurantRepository.findAllByLocation(location);
	}
	
}

