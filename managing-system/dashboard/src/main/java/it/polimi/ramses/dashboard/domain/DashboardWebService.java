package it.polimi.ramses.dashboard.domain;

import it.polimi.ramses.dashboard.externalinterfaces.AnalyseClient;
import it.polimi.ramses.dashboard.externalinterfaces.KnowledgeClient;
import it.polimi.ramses.dashboard.externalinterfaces.MonitorClient;
import it.polimi.ramses.dashboard.externalinterfaces.PlanClient;
import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.ramses.knowledge.domain.architecture.Instance;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
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
	@Autowired
	private AnalyseClient analyseClient;
	@Autowired
	private PlanClient planClient;

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

	public Modules getFailedModule() {
		return knowledgeClient.getFailedModule();
	}


	// Configuration methods
	// MONITOR
	public MonitorClient.GetInfoResponse getMonitorInfo() {
		return monitorClient.getInfo();
	}
	public void changeMonitorSchedulingPeriod(int period) {
		monitorClient.changeSchedulingPeriod(period);
	}
	public void startMonitorRoutine() {
		monitorClient.startRoutine();
	}
	public void stopMonitorRoutine() {
		monitorClient.stopRoutine();
	}

	// ANALYSE
	public AnalyseClient.GetInfoResponse getAnalyseInfo() {
		return analyseClient.getInfo();
	}
	public void changeMetricsWindowSize(int value) {
		analyseClient.changeMetricsWindowSize(value);
	}
	public void changeAnalysisWindowSize(int value) {
		analyseClient.changeAnalysisWindowSize(value);
	}
	public void changeFailureRateThreshold(double value) {
		analyseClient.changeFailureRateThreshold(value);
	}
	public void changeUnreachableRateThreshold(double value) {
		analyseClient.changeUnreachableRateThreshold(value);
	}
	public void changeQoSSatisfactionRate(double value) {
		analyseClient.changeQoSSatisfactionRate(value);
	}

	public boolean isAdaptationEnabled() {
		return Boolean.parseBoolean(planClient.getAdaptationStatus());
	}
	public void changeAdaptationStatus(boolean isAdaptationEnabled) {
		planClient.setAdaptationStatus(isAdaptationEnabled);
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
