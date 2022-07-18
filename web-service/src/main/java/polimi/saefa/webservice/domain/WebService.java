package polimi.saefa.webservice.domain;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.web.bind.annotation.GetMapping;
import polimi.saefa.restaurantservice.restapi.common.*;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class WebService {
	private final Logger logger = Logger.getLogger(WebService.class.toString());
	@Autowired
	private EurekaClient discoveryClient;

	private String getServiceUrl(String serviceName) {
		InstanceInfo instance = discoveryClient.getNextServerFromEureka(serviceName, false);
		return instance.getHomePageUrl();
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() throws RestClientException, IOException {
		String rsUrl = getServiceUrl("API-GATEWAY-SERVICE")+"RESTAURANT-SERVICE/rest/customer/";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantsResponse> response = restTemplate.exchange(rsUrl+"restaurants", HttpMethod.GET, getHeaders(), GetRestaurantsResponse.class);
		logger.info("getAllRestaurants: " + response.getBody());
		Collection<GetRestaurantResponse> restaurants = response.getBody().getRestaurants();
		return restaurants;
	}

	public GetRestaurantResponse getRestaurant(Long id) throws IOException {
		String rsUrl = getServiceUrl("API-GATEWAY-SERVICE")+"RESTAURANT-SERVICE/rest/customer/";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantResponse> response = restTemplate.exchange(rsUrl+"restaurants/"+id.toString(), HttpMethod.GET, getHeaders(), GetRestaurantResponse.class);
		logger.info("getRestaurant: " + response.getBody());
		return response.getBody();
	}


	/*
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
		RestaurantMenu menu = new RestaurantMenu(menuItems);
		restaurant.setMenu(menu); 
		restaurant = restaurantRepository.save(restaurant);
		return restaurant;
	}

 	public RestaurantMenu getRestaurantMenu(Long id) {
		Restaurant restaurant = restaurantRepository.findByIdWithMenu(id);
		RestaurantMenu menu = restaurant.getMenu();
		return menu; 
	}

 	public Restaurant getRestaurantByName(String name) {
		Restaurant restaurant = restaurantRepository.findByName(name);
		return restaurant;
	}
	
	public Collection<Restaurant> getAllRestaurantsByLocation(String location) {
		Collection<Restaurant> restaurants = restaurantRepository.findAllByLocation(location);
		return restaurants;
	}
	*/


	private static HttpEntity<?> getHeaders() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

