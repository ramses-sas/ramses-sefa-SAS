package polimi.saefa.deliveryproxy3service.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import polimi.saefa.deliveryproxy3service.externalinterface.DeliverRequest;

import java.util.Date;

@Service
public class DeliveryProxyService {
	@Value("${delivery.service3.uri}")
	private String deliveryServiceUri;

 	public boolean deliverOrder(
		 String address,
		 String city,
		 int number,
		 String zipcode,
		 String telephoneNumber,
		 Date scheduledTime
	) {
		String url = deliveryServiceUri+"/deliver/";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(url, new DeliverRequest(address, city, number, zipcode, telephoneNumber, scheduledTime), String.class);
		return response.getStatusCode().is2xxSuccessful();
	 }
	
}

