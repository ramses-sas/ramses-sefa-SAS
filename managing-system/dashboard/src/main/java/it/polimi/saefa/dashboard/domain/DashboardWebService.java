package it.polimi.saefa.dashboard.domain;

import it.polimi.saefa.dashboard.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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
		getAllServices().forEach(s -> currentArchitecture.put(s.getServiceId(), s));
		return currentArchitecture;
	}
}

