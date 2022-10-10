package it.polimi.saefa.dashboard.domain;

import it.polimi.saefa.dashboard.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
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
		return knowledgeClient.getServicesMap();
	}

	public InstanceMetricsSnapshot getLatestMetrics(String serviceId, String instanceId) {
		List<InstanceMetricsSnapshot> l = knowledgeClient.getLatestMetrics(serviceId, instanceId);
		if (l == null || l.isEmpty())
			return null;
		return l.get(0);
	}

	public void breakpoint(){
		log.info("breakpoint");
	}
}

