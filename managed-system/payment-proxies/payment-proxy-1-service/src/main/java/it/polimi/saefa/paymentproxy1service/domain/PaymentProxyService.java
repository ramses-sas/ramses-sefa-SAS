package it.polimi.saefa.paymentproxy1service.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import it.polimi.saefa.paymentproxy1service.externalinterface.PaymentRequest;

@Service
public class PaymentProxyService {
	@Value("${payment.service.uri}")
	private String paymentServiceUri;

	public boolean processPayment(
		String cardNumber,
		int expMonth,
		int expYear,
		String cvv,
		double amount
	) {
		String url = paymentServiceUri+"/pay/";
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.postForEntity(url, new PaymentRequest(cardNumber, expMonth, expYear, cvv, amount), String.class);
		return response.getStatusCode().is2xxSuccessful();
	}
	
}

