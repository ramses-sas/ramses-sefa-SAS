package polimi.saefa.webservice.domain.customer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import polimi.saefa.restaurantservice.restapi.common.*;
import polimi.saefa.orderingservice.restapi.*;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

@Service
public class CustomerWebService {
	private final Logger logger = Logger.getLogger(CustomerWebService.class.toString());
	@Autowired
	private EurekaClient discoveryClient;

	private String getServiceUrl(String serviceName) {
		InstanceInfo instance = discoveryClient.getNextServerFromEureka("API-GATEWAY-SERVICE", false);
		return instance.getHomePageUrl()+serviceName+"/";
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() {
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


	public AddItemToCartResponse addItemToCart(Long cartId, Long restaurantId, String itemId, int quantity) {
		String osUrl = getServiceUrl("ORDERING-SERVICE")+"rest/";
		String url = osUrl+"addItem/";
		RestTemplate restTemplate = new RestTemplate();
		//TODO
		ResponseEntity<AddItemToCartResponse> response =
			restTemplate.postForEntity(url, new AddItemToCartRequest(cartId,restaurantId,itemId,quantity), AddItemToCartResponse.class);
		return response.getBody();
	}

	public RemoveItemFromCartResponse removeItemFromCart(Long cartId, Long restaurantId, String itemId, int quantity) {
		String osUrl = getServiceUrl("ORDERING-SERVICE")+"rest/";
		String url = osUrl+"removeItem/";
		RestTemplate restTemplate = new RestTemplate();
		//TODO
		ResponseEntity<RemoveItemFromCartResponse> response =
			restTemplate.postForEntity(url, new RemoveItemFromCartRequest(cartId,restaurantId,itemId,quantity), RemoveItemFromCartResponse.class);
		return response.getBody();
	}

	public GetCartResponse getCart(Long cartId) {
		String osUrl = getServiceUrl("ORDERING-SERVICE")+"rest/";
		String url = osUrl+"getCart/"+cartId.toString();
		RestTemplate restTemplate = new RestTemplate();
		//TODO
		ResponseEntity<GetCartResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetCartResponse.class);
		return response.getBody();
	}

	public ConfirmOrderResponse confirmOrder(
		Long cartId,
		String cardNumber,
		int expMonth,
		int expYear,
		String cvv,
		String address,
		String city,
		int number,
		String zipcode,
		String telephoneNumber,
		Date scheduledTime
	) {
		String osUrl = getServiceUrl("ORDERING-SERVICE")+"rest/";
		String url = osUrl+"confirmOrder/";
		RestTemplate restTemplate = new RestTemplate();
		//TODO
		ResponseEntity<ConfirmOrderResponse> response =
			restTemplate.postForEntity(url,
				new ConfirmOrderRequest(cartId,cardNumber,expMonth,expYear,cvv,address,city,number,zipcode,telephoneNumber,scheduledTime),
				ConfirmOrderResponse.class);
		return response.getBody();
	}

	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

