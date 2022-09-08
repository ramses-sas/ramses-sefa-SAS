package it.polimi.saefa.dashboard.domain;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;

import java.util.*;


@Slf4j
@org.springframework.stereotype.Service
public class DashboardWebService {

	public Map<String, Service> getConfiguration() {
		Map<String, Service> currentConfiguration = new HashMap<>();
		// TODO: implement. Call Knowledge REST endpoint (to create)
		Service test = new Service("PAYMENT-PROXY-SERVICE");
		Instance i = new Instance("payment-proxy-1-service@localhost:00000", test);
		ServiceConfiguration sc = new ServiceConfiguration();
		sc.setTimestamp(new Date());
		test.addInstance(i);
		test.setCurrentImplementation("payment-proxy-1-service");
		test.setConfiguration(sc);
		currentConfiguration.put("PAYMENT-PROXY-SERVICE", test);
		return currentConfiguration;
	}

	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

