package polimi.saefa.webservice.domain.customer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantMenuResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantResponse;
import polimi.saefa.restaurantservice.restapi.common.GetRestaurantsResponse;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Logger;

@Service
@Transactional
public class CustomerWebService {
	private final Logger logger = Logger.getLogger(CustomerWebService.class.toString());
	@Autowired
	private EurekaClient discoveryClient;

	private String getServiceUrl(String serviceName) {
		InstanceInfo instance = discoveryClient.getNextServerFromEureka("API-GATEWAY-SERVICE", false);
		return instance.getHomePageUrl()+serviceName+"/";
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() throws RestClientException {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/customer/";
		logger.warning("GET "+rsUrl);
		String url = rsUrl+"restaurants";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantsResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantsResponse.class);
		return Objects.requireNonNull(response.getBody()).getRestaurants();
	}

	public GetRestaurantResponse getRestaurant(Long id) {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/customer/";
		String url = rsUrl+"restaurants/"+id.toString();
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantResponse.class);
		return response.getBody();
	}

	public GetRestaurantMenuResponse getRestaurantMenu(Long id) {
		String rsUrl = getServiceUrl("RESTAURANT-SERVICE")+"rest/customer/";
		String url = rsUrl+"restaurants/"+id.toString()+"/menu";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantMenuResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantMenuResponse.class);
		return response.getBody();
	}

	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

