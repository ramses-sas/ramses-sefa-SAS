package it.polimi.saefa.dashboard.domain;

import it.polimi.saefa.dashboard.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.dashboard.externalinterfaces.MonitorClient;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
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
	@Autowired
	private MonitorClient monitorClient;

	public Service getService(String serviceId) {
		return knowledgeClient.getService(serviceId);
	}

	public Instance getInstance(String serviceId, String instanceId) {
		return knowledgeClient.getInstance(serviceId, instanceId);
	}

	public Date getServiceLatestAdaptationDate(String serviceId) {
		return knowledgeClient.getServiceLatestAdaptationDate(serviceId);
	}

	public Map<String, Service> getArchitecture() {
		return knowledgeClient.getServicesMap();
	}

	public Modules getActiveModule() {
		return knowledgeClient.getActiveModule();
	}

	public Map<String, List<AdaptationOption>> getChosenAdaptationOptionsHistory(int n) {
		return knowledgeClient.getChosenAdaptationOptionsHistory(n);
	}


	// Configuration methods
	// MONITOR
	public int getMonitorSchedulingPeriod() {
		return monitorClient.getSchedulingPeriod();
	}
	public void changeMonitorSchedulingPeriod(int period) {
		monitorClient.changeSchedulingPeriod(period);
	}


	public void breakpoint(){
		log.info("breakpoint");
	}
}


/*
	public InstanceMetricsSnapshot getLatestMetrics(String serviceId, String instanceId) {
		List<InstanceMetricsSnapshot> l = knowledgeClient.getLatestMetrics(serviceId, instanceId);
		if (l == null || l.isEmpty())
			return null;
		return l.get(0);
	}

	public Map<String, List<AdaptationOption>> getProposedAdaptationOptions() {
		return knowledgeClient.getProposedAdaptationOptions();
	}

	public Map<String, List<AdaptationOption>> getChosenAdaptationOptions() {
		return knowledgeClient.getChosenAdaptationOptions();
	}


 */

