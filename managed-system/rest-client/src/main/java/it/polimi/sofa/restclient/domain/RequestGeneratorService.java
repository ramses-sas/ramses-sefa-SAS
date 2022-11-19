package it.polimi.sofa.restclient.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import it.polimi.sofa.restaurantservice.restapi.common.*;
import it.polimi.sofa.orderingservice.restapi.*;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.logging.Logger;

@Slf4j
@Service
public class RequestGeneratorService {
	@Value("${API_GATEWAY_IP_PORT}")
	private String apiGatewayUri;

	private String getApiGatewayUrl() {
		return "http://"+apiGatewayUri;
	}

	public Collection<GetRestaurantResponse> getAllRestaurants() {
		String url = getApiGatewayUrl()+"/customer/restaurants";
		RestTemplate restTemplate = new RestTemplate();
		GetRestaurantsResponse response = restTemplate.getForObject(url, GetRestaurantsResponse.class);
		return Objects.requireNonNull(response).getRestaurants();
	}

	public GetRestaurantResponse getRestaurant(Long id) {
		String url = getApiGatewayUrl()+"/customer/restaurants/"+id.toString();
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(url, GetRestaurantResponse.class);
	}

	public GetRestaurantMenuResponse getRestaurantMenu(Long id) {
		String url = getApiGatewayUrl()+"/customer/restaurants/"+id.toString()+"/menu";
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(url, GetRestaurantMenuResponse.class);
	}


	public CreateCartResponse createCart(Long restaurantId) {
		String url = getApiGatewayUrl()+"/customer/cart/";
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.postForObject(url, new CreateCartRequest(restaurantId), CreateCartResponse.class);
	}


	public AddItemToCartResponse addItemToCart(Long cartId, Long restaurantId, String itemId, int quantity) {
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId+"/addItem";
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.postForObject(url, new AddItemToCartRequest(cartId,restaurantId,itemId,quantity), AddItemToCartResponse.class);
	}

	public GetCartResponse getCart(Long cartId) {
		String url = getApiGatewayUrl()+"/customer/cart/"+cartId;
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(url, GetCartResponse.class);
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
		ConfirmOrderResponse response = restTemplate.postForObject(url,
				new ConfirmOrderRequest(cartId,cardNumber,expMonth,expYear,cvv,address,city,number,zipcode,telephoneNumber,scheduledTime),
				ConfirmOrderResponse.class);
		return response;
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
		ConfirmOrderResponse response = restTemplate.postForObject(url,
				new ConfirmCashPaymentRequest(address,city,number,zipcode,telephoneNumber,scheduledTime),
				ConfirmOrderResponse.class);
		return response;
	}

	public ConfirmOrderResponse handleTakeAway(
			Long cartId,
			boolean confirmed
	) {
		String url = getApiGatewayUrl()+"/customer/cart/" + cartId + (confirmed ? "/confirmTakeAway": "/rejectTakeAway");
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.postForObject(url, null, ConfirmOrderResponse.class);
	}

}

