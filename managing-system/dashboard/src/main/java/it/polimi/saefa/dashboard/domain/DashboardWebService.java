package it.polimi.saefa.dashboard.domain;

import it.polimi.saefa.dashboard.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.*;


@Slf4j
@org.springframework.stereotype.Service
public class DashboardWebService {
	@Autowired
	private KnowledgeClient knowledgeClient;

	public List<Service> getAllServices() {
		return knowledgeClient.getServices();
	}

	public Service getService(String serviceId) {
		return knowledgeClient.getService(serviceId);
	}

	public Map<String, Service> getArchitecture() {
		Map<String, Service> currentArchitecture = new HashMap<>();
		// TODO: implement. Call Knowledge REST endpoint (to create)
		/*Service test = new Service("PAYMENT-PROXY-SERVICE");
		Instance i = new Instance("payment-proxy-1-service@localhost:00000", "PAYMENT-PROXY-SERVICE");
		ServiceConfiguration sc = new ServiceConfiguration();
		sc.setTimestamp(new Date());
		test.addInstance(i);
		test.setCurrentImplementation("payment-proxy-1-service");
		test.setConfiguration(sc);
		currentArchitecture.put("PAYMENT-PROXY-SERVICE", test);*/
		getAllServices().forEach(s -> currentArchitecture.put(s.getServiceId(), s));
		return currentArchitecture;
	}

	private static HttpEntity<?> getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return new HttpEntity<>(headers);
	}
}

