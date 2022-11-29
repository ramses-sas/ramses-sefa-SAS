package it.polimi.sefa.webservice.domain.admin;

import java.util.*;

import it.polimi.sefa.restaurantservice.restapi.admin.CreateRestaurantMenuRequest;
import it.polimi.sefa.restaurantservice.restapi.admin.CreateRestaurantRequest;
import it.polimi.sefa.restaurantservice.restapi.admin.CreateRestaurantResponse;
import it.polimi.sefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import it.polimi.sefa.restaurantservice.restapi.common.GetRestaurantResponse;
import it.polimi.sefa.restaurantservice.restapi.common.GetRestaurantsResponse;
import it.polimi.sefa.restaurantservice.restapi.common.MenuItemElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AdminWebService {
	@Value("${API_GATEWAY_IP_PORT}")
	private String apiGatewayUri;

	private String getApiGatewayUrl() {
		return "http://"+apiGatewayUri;
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() throws RestClientException {
		String url = getApiGatewayUrl()+"/admin/restaurants";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantsResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantsResponse.class);
		return Objects.requireNonNull(response.getBody()).getRestaurants();
	}

	public GetRestaurantResponse getRestaurant(Long id) {
		String url = getApiGatewayUrl()+"/admin/restaurants/"+id.toString();
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantResponse.class);
		return response.getBody();
	}

	public GetRestaurantMenuResponse getRestaurantMenu(Long id) {
		String url = getApiGatewayUrl()+"/admin/restaurants/"+id.toString()+"/menu";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantMenuResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantMenuResponse.class);
		return response.getBody();
	}

 	public CreateRestaurantResponse createRestaurant(String name, String location) {
		String url = getApiGatewayUrl()+"/admin/restaurants";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<CreateRestaurantResponse> response = restTemplate.postForEntity(url, new CreateRestaurantRequest(name, location), CreateRestaurantResponse.class);
		return response.getBody();
	}

 	public void createOrUpdateRestaurantMenu(Long id, List<MenuItemElement> menuItems) {
		String url = getApiGatewayUrl()+"/admin/restaurants/"+id.toString()+"/menu";
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.put(url, new CreateRestaurantMenuRequest(id, menuItems));
	}


	// NOT USED
	/*
 	public Restaurant getRestaurantByName(String name) {
		Restaurant restaurant = restaurantRepository.findByName(name);
		return restaurant;
	}
	
	public Collection<Restaurant> getAllRestaurantsByLocation(String location) {
		Collection<Restaurant> restaurants = restaurantRepository.findAllByLocation(location);
		return restaurants;
	}
	*/


	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

