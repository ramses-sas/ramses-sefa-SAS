package polimi.saefa.webservice.domain.admin;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import java.util.*;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import polimi.saefa.restaurantservice.restapi.admin.CreateRestaurantMenuRequest;
import polimi.saefa.restaurantservice.restapi.admin.CreateRestaurantRequest;
import polimi.saefa.restaurantservice.restapi.admin.CreateRestaurantResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantsResponse;
import polimi.saefa.restaurantservice.restapi.common.MenuItemElement;

@Service
public class AdminWebService {
	private final Logger logger = Logger.getLogger(AdminWebService.class.toString());
	@Autowired
	private EurekaClient discoveryClient;

	private String getServiceUrl(String serviceName) {
		InstanceInfo instance = discoveryClient.getNextServerFromEureka("API-GATEWAY-SERVICE", false);
		return instance.getHomePageUrl()+serviceName+"/";
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() throws RestClientException {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/admin/";
		String url = rsUrl+"restaurants";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantsResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantsResponse.class);
		return Objects.requireNonNull(response.getBody()).getRestaurants();
	}

	public GetRestaurantResponse getRestaurant(Long id) {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/admin/";
		String url = rsUrl+"restaurants/"+id.toString();
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantResponse.class);
		return response.getBody();
	}

	public GetRestaurantMenuResponse getRestaurantMenu(Long id) {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/admin/";
		String url = rsUrl+"restaurants/"+id.toString()+"/menu";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantMenuResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantMenuResponse.class);
		return response.getBody();
	}

 	public CreateRestaurantResponse createRestaurant(String name, String location) {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/admin/";
		String url = rsUrl+"restaurants";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<CreateRestaurantResponse> response = restTemplate.postForEntity(url, new CreateRestaurantRequest(name, location), CreateRestaurantResponse.class);
		return response.getBody();
	}

 	public void createOrUpdateRestaurantMenu(Long id, List<MenuItemElement> menuItems) {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/admin/";
		String url = rsUrl+"restaurants/"+id.toString()+"/menu";
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

