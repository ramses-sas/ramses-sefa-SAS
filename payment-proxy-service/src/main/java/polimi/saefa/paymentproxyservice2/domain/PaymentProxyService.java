package polimi.saefa.paymentproxyservice2.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import polimi.saefa.paymentproxyservice2.externalinterface.PaymentRequest;

@Service
@Transactional
public class PaymentProxyService {
	@Value("${payment.service2.uri}")
	private String paymentServiceUri;

	public void processPayment(
		String cardNumber,
		int expMonth,
		int expYear,
		String cvv,
		double amount
	) {
		String url = paymentServiceUri+"pay/";
		RestTemplate restTemplate = new RestTemplate();
		//TODO
		restTemplate.postForEntity(url, new PaymentRequest(cardNumber, expMonth, expYear, cvv, amount), String.class);
	}
	
}

