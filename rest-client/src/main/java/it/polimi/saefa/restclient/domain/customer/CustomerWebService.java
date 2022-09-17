package it.polimi.saefa.restclient.domain.customer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import it.polimi.saefa.restaurantservice.restapi.common.*;
import it.polimi.saefa.orderingservice.restapi.*;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

@Service
public class CustomerWebService {
	@Value("${API_GATEWAY_IP_PORT}")
	private String apiGatewayUri;

	Logger logger = Logger.getLogger(CustomerWebService.class.toString());

	private String getApiGatewayUrl() {
		return "http://"+apiGatewayUri;
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() {
		String url = getApiGatewayUrl()+"/customer/restaurants";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantsResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantsResponse.class);
		return Objects.requireNonNull(response.getBody()).getRestaurants();
	}

	public GetRestaurantResponse getRestaurant(Long id) {
		String url = getApiGatewayUrl()+"/customer/restaurants/"+id.toString();
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantResponse.class);
		return response.getBody();
	}

	public GetRestaurantMenuResponse getRestaurantMenu(Long id) {
		String url = getApiGatewayUrl()+"/customer/restaurants/"+id.toString()+"/menu";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<GetRestaurantMenuResponse> response = restTemplate.exchange(url, HttpMethod.GET, getHeaders(), GetRestaurantMenuResponse.class);
		return response.getBody();
	}


	public CreateCartResponse createCart(Long restaurantId) {
		String url = getApiGatewayUrl()+"/customer/cart/";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<CreateCartResponse> response = restTemplate.postForEntity(url, new CreateCartRequest(restaurantId), CreateCartResponse.class);
		return response.getBody();
	}


	public AddItemToCartResponse addItemToCart(Long cartId, Long restaurantId, String itemId, int quantity) {
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId+"/addItem";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<AddItemToCartResponse> response =
			restTemplate.postForEntity(url, new AddItemToCartRequest(cartId,restaurantId,itemId,quantity), AddItemToCartResponse.class);
		return response.getBody();
	}

	// NOT USED
	public RemoveItemFromCartResponse removeItemFromCart(Long cartId, Long restaurantId, String itemId, int quantity) {
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId+"/removeItem";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<RemoveItemFromCartResponse> response =
			restTemplate.postForEntity(url, new RemoveItemFromCartRequest(cartId,restaurantId,itemId,quantity), RemoveItemFromCartResponse.class);
		return response.getBody();
	}

	public GetCartResponse getCart(Long cartId) {
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId;
		RestTemplate restTemplate = new RestTemplate();
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
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId+"/confirmOrder";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<ConfirmOrderResponse> response =
			restTemplate.postForEntity(url,
				new ConfirmOrderRequest(cartId,cardNumber,expMonth,expYear,cvv,address,city,number,zipcode,telephoneNumber,scheduledTime),
				ConfirmOrderResponse.class);
		return response.getBody();
	}

	public ConfirmOrderResponse confirmCashPayment(
			Long cartId,
			String address,
			String city,
			int number,
			String zipcode,
			String telephoneNumber,
			Date scheduledTime
	) {
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId+"/confirmCashPayment";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<ConfirmOrderResponse> response =
				restTemplate.postForEntity(url,
						new ConfirmCashPaymentRequest(address,city,number,zipcode,telephoneNumber,scheduledTime),
						ConfirmOrderResponse.class);
		return response.getBody();
	}

	public ConfirmOrderResponse handleTakeAway(
			Long cartId,
			boolean confirmed
	) {
		String url = getApiGatewayUrl()+"/customer/cart/" + cartId + (confirmed ? "/confirmTakeAway": "/rejectTakeAway");
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<ConfirmOrderResponse> response =
				restTemplate.postForEntity(url, null, ConfirmOrderResponse.class);
		return response.getBody();
	}

	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

