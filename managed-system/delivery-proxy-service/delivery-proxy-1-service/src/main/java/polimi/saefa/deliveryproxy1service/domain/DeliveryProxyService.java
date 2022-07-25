package polimi.saefa.deliveryproxy1service.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import polimi.saefa.deliveryproxy1service.externalinterface.DeliverRequest;

import java.util.Date;

@Service
@Transactional
public class DeliveryProxyService {
	@Value("${delivery.service1.uri}")
	private String deliveryServiceUri;

 	public void deliverOrder(
		 String address,
		 String city,
		 int number,
		 String zipcode,
		 String telephoneNumber,
		 Date scheduledTime
	) {
		String url = deliveryServiceUri+"deliver/";
		RestTemplate restTemplate = new RestTemplate();
		//TODO
		restTemplate.postForEntity(url, new DeliverRequest(address, city, number, zipcode, telephoneNumber, scheduledTime), String.class);
	}
	
}

