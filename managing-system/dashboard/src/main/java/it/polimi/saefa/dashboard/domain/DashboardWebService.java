package it.polimi.saefa.dashboard.domain;

import it.polimi.saefa.knowledge.persistence.domain.Instance;
import it.polimi.saefa.knowledge.persistence.domain.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@org.springframework.stereotype.Service
public class DashboardWebService {

	public Map<String, Service> getConfiguration() {
		Map<String, Service> currentConfiguration = new HashMap<>();
		// TODO: implement. Call Knowledge REST endpoint (to create)
		Service test = new Service("PAYMENT-PROXY-SERVICE", "payment-proxy-1-service");
		test.addInstance(new Instance("payment-proxy-1-service", test));
		currentConfiguration.put("PAYMENT-PROXY-SERVICE", test);
		return currentConfiguration;
	}

	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

