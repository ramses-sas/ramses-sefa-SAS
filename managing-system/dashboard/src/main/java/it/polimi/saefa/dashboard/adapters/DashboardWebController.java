package it.polimi.saefa.dashboard.adapters;

import it.polimi.saefa.dashboard.domain.DashboardWebService;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParameter;
import it.polimi.saefa.knowledge.domain.architecture.Instance;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.domain.architecture.ServiceImplementation;
import it.polimi.saefa.knowledge.domain.metrics.CircuitBreakerMetrics;
import it.polimi.saefa.knowledge.domain.metrics.HttpEndpointMetrics;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@Slf4j
public class DashboardWebController {
	@Value("${MAX_HISTORY_SIZE}")
	private int maxHistorySize;

	@Autowired 
	private DashboardWebService dashboardWebService;



	/* Mostra home page */
	@GetMapping("/")
	public String index(Model model) {
		Collection<Service> services = dashboardWebService.getArchitecture().values();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		// <serviceId, [[Property, Value]]>
		Map<String, List<String[]>> servicesConfigurationTable = new HashMap<>();
		// <serviceId, [[ParameterName, Value, Threshold, Weight, Priority]]>
		Map<String, List<String[]>> servicesAdaptationParametersTable = new HashMap<>();
		Map<String, List<String[]>> servicesCurrentImplementationTable = new HashMap<>();
		for (Service s : services) {
			ServiceConfiguration conf = s.getConfiguration();
			// List <CustomPropertyName, Value>
			List<String[]> table = new ArrayList<>();
			table.add(new String[]{"Time Of Snapshot", sdf.format(conf.getTimestamp())+" UTC"});
			table.add(new String[]{"", ""});
			table.add(new String[]{"Load Balancer Type", conf.getLoadBalancerType().name().replace("_", " ")});
			table.add(new String[]{"", ""});
			for (ServiceConfiguration.CircuitBreakerConfiguration cbConf : conf.getCircuitBreakersConfiguration().values()) {
				table.add(new String[]{"Circuit Breaker Name", cbConf.getCircuitBreakerName()});
				table.add(new String[]{"Failure Rate Threshold", cbConf.getFailureRateThreshold()+"%"});
				table.add(new String[]{"Slow Call Rate Threshold", cbConf.getSlowCallRateThreshold()+"%"});
				table.add(new String[]{"Slow Call Duration Threshold", cbConf.getSlowCallDurationThreshold()+"ms"});
				table.add(new String[]{"Minimum Number Of Calls", String.valueOf(cbConf.getMinimumNumberOfCalls())});
				table.add(new String[]{"Number Of Calls In Half-Open", cbConf.getPermittedNumberOfCallsInHalfOpenState().toString()});
				table.add(new String[]{"Wait Duration In Open", cbConf.getWaitDurationInOpenState() +"ms"});
				table.add(new String[]{"Event Consumer Buffer Size", String.valueOf(cbConf.getEventConsumerBufferSize())});
				table.add(new String[]{"Sliding Window Size", String.valueOf(cbConf.getSlidingWindowSize())});
				table.add(new String[]{"Sliding Window Type", String.valueOf(cbConf.getSlidingWindowType()).replace("_", " ")});
				table.add(new String[]{"", ""});
			}
			table.remove(table.size() - 1);
			servicesConfigurationTable.put(s.getServiceId(), table);

			// List <ParameterName, Value, Threshold, Weight>
			List<String[]> serviceAdaptationParametersTable = new ArrayList<>();
			for (AdaptationParameter<? extends AdaptationParamSpecification> ap : s.getCurrentImplementation().getAdaptationParamCollection().getAdaptationParamsMap().values()) {
				serviceAdaptationParametersTable.add(new String[]{
						ap.getSpecification().getClass().getSimpleName(),
						ap.getCurrentValue() == null ? "N/A" : String.format(Locale.ROOT,"%.3f", ap.getCurrentValue().getValue()),
						ap.getSpecification().getConstraintDescription(),
						ap.getSpecification().getWeight().toString()}
				);
			}
			servicesAdaptationParametersTable.put(s.getServiceId(), serviceAdaptationParametersTable);

			ServiceImplementation currentImplementation = s.getCurrentImplementation();
			// [[ImplementationId, CostPerBoot, CostPerInstance, ...]]
			List<String[]> currentImplementationTable = new ArrayList<>();
			currentImplementationTable.add(new String[]{"Implementation Id", currentImplementation.getImplementationId()});
			currentImplementationTable.add(new String[]{"Cost Per Boot", currentImplementation.getCostPerBoot()+"€"});
			currentImplementationTable.add(new String[]{"Cost Per Instance", currentImplementation.getCostPerInstance()+"€"});
			currentImplementationTable.add(new String[]{"Cost Per Second Of Execution", currentImplementation.getCostPerSecond()+"€/s"});
			currentImplementationTable.add(new String[]{"Cost Per Request", currentImplementation.getCostPerRequest()+"€/request"});
			servicesCurrentImplementationTable.put(s.getServiceId(), currentImplementationTable);
		}

		model.addAttribute("servicesIds", services.stream().map(Service::getServiceId).toList());
		model.addAttribute("servicesConfigurationTable", servicesConfigurationTable);
		model.addAttribute("servicesAdaptationParametersTable", servicesAdaptationParametersTable);
		model.addAttribute("servicesCurrentImplementationTable", servicesCurrentImplementationTable);
		return "index";
	}


	@GetMapping("/service/{serviceId}")
	public String serviceDetails(Model model, @PathVariable String serviceId) {
		Service service = dashboardWebService.getService(serviceId);
		log.info("Service: " + service);
		// [[InstanceId, Status, LatestMetricsDescription]]
		List<String[]> instancesTable = new ArrayList<>();
		for (Instance instance : service.getInstances())
			instancesTable.add(new String[]{instance.getInstanceId(), instance.getCurrentStatus().toString()});
		model.addAttribute("serviceId", serviceId);
		model.addAttribute("possibleImplementations", service.getPossibleImplementations().keySet());
		model.addAttribute("instancesTable", instancesTable);

		model.addAttribute("graphs", computeServiceGraphs(service));
		return "webpages/serviceDetails";
	}

	@GetMapping("/service/{serviceId}/{instanceId}")
	public String instanceDetails(Model model, @PathVariable String serviceId, @PathVariable String instanceId) {
		Instance instance = dashboardWebService.getInstance(serviceId, instanceId);
		InstanceMetricsSnapshot latestMetrics = instance.getLatestInstanceMetricsSnapshot();
		List<String[]> resourceTable = new ArrayList<>();
		List<String[]> httpMetricsTable = new ArrayList<>();
		List<String[]> circuitBreakersTable = new ArrayList<>();
		if (latestMetrics != null) {
			resourceTable.add(new String[]{"CPU Usage", "" + String.format(Locale.ROOT, "%.2f", latestMetrics.getCpuUsage()*100)+"%"});
			resourceTable.add(new String[]{"Disk Free Space", String.format(Locale.ROOT, "%.2f", latestMetrics.getDiskFreeSpace()/1024/1024/1024)+" GB"});
			resourceTable.add(new String[]{"Disk Total Space", String.format(Locale.ROOT, "%.2f", latestMetrics.getDiskTotalSpace()/1024/1024/1024)+" GB"});
			for (HttpEndpointMetrics httpMetrics : latestMetrics.getHttpMetrics().values())
				for(String outcome : httpMetrics.getOutcomes())
					httpMetricsTable.add(new String[]{httpMetrics.getHttpMethod() + " " + httpMetrics.getEndpoint(),
						outcome, String.valueOf(httpMetrics.getCountByOutcome(outcome)),
							httpMetrics.getAverageDurationByOutcome(outcome)==-1 ? "N/A" : String.format(Locale.ROOT,"%.1f", httpMetrics.getAverageDurationByOutcome(outcome)*1000)+" ms"});
			for (CircuitBreakerMetrics cbMetrics : latestMetrics.getCircuitBreakerMetrics().values()) {
				circuitBreakersTable.add(new String[]{"Circuit Breaker Name", cbMetrics.getName()});
				double failureRate = cbMetrics.getFailureRate();
				circuitBreakersTable.add(new String[]{"Failure Rate", failureRate==-1 ? "N/A" : String.format(Locale.ROOT, "%.1f", failureRate)+"%"});
				circuitBreakersTable.add(new String[]{"Failed Calls Count", cbMetrics.getNotPermittedCallsCount()+""});
				double slowCallRate = cbMetrics.getSlowCallRate();
				circuitBreakersTable.add(new String[]{"Slow Calls Rate", slowCallRate==-1 ? "N/A" : String.format(Locale.ROOT, "%.1f", slowCallRate)+"%"});
				circuitBreakersTable.add(new String[]{"Slow Calls Count", cbMetrics.getSlowCallCount()+""});
				for (CircuitBreakerMetrics.CallOutcomeStatus status : CircuitBreakerMetrics.CallOutcomeStatus.values()) {
					Double avgDuration = cbMetrics.getAverageDuration(status);
					circuitBreakersTable.add(new String[]{"Average Call Duration when "+status, avgDuration.isNaN() ? "N/A" : String.format(Locale.ROOT, "%.3f", avgDuration)+" s"});
				}
				circuitBreakersTable.add(new String[]{"", ""});
			}
		}
		model.addAttribute("resourceTable", resourceTable);
		model.addAttribute("httpMetricsTable", httpMetricsTable);
		model.addAttribute("circuitBreakersTable", circuitBreakersTable);
		model.addAttribute("graphs", computeInstanceGraphs(instance, dashboardWebService.getServiceLatestAdaptationDate(serviceId)));
		return "webpages/instanceDetails";
	}



	/* Display current status */
	@GetMapping("/adaptation")
	public String loopStatus(Model model) {
		Modules activeModule = dashboardWebService.getActiveModule();
		switch (activeModule) {
			case MONITOR:
				return "webpages/monitorActive";
			case ANALYSE:
				return "webpages/analyseActive";
			case PLAN:
				Map<String, List<AdaptationOption>> proposedAdaptationOptions = dashboardWebService.getProposedAdaptationOptions();
				model.addAttribute("proposedAdaptationOptions", proposedAdaptationOptions);
				return "webpages/planActive";
			case EXECUTE:
				Map<String, List<AdaptationOption>> chosenAdaptationOptions = dashboardWebService.getChosenAdaptationOptions();
				model.addAttribute("chosenAdaptationOptions", chosenAdaptationOptions);
				return "webpages/executeActive";
		}
		return "index";
	}



	private GraphData[] computeServiceGraphs(Service service) {
		GraphData[] graphs = new GraphData[2];
		List<AdaptationParameter.Value> values;
		int valuesSize, oldestValueIndex;

		GraphData availabilityGraph = new GraphData("Instant", "Availability");
		values = service.getValuesHistoryForParam(Availability.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			AdaptationParameter.Value v = values.get(oldestValueIndex-i);
			availabilityGraph.addPoint(v.getTimestamp().before(service.getLatestAdaptationDate()) ? "B"+(i+1) : "A"+(i+1), v.getValue());
			i++;
		}
		graphs[0] = availabilityGraph;

		GraphData artGraph = new GraphData("Instant", "Average Response Time [ms]");
		values = service.getValuesHistoryForParam(AverageResponseTime.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			AdaptationParameter.Value v = values.get(oldestValueIndex-i);
			artGraph.addPoint(v.getTimestamp().before(service.getLatestAdaptationDate()) ? "B"+(i+1) : "A"+(i+1), v.getValue()*1000);
		}
		graphs[1] = artGraph;

		return graphs;
	}

	private GraphData[] computeInstanceGraphs(Instance instance, Date serviceLatestAdaptationDate) {
		GraphData[] graphs = new GraphData[2];
		List<AdaptationParameter.Value> values;
		int valuesSize, oldestValueIndex;

		GraphData availabilityGraph = new GraphData("Instant", "Availability");
		values = instance.getAdaptationParamCollection().getValuesHistoryForParam(Availability.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			AdaptationParameter.Value v = values.get(oldestValueIndex-i);
			availabilityGraph.addPoint(v.getTimestamp().before(serviceLatestAdaptationDate) ? "B"+(i+1) : "A"+(i+1), v.getValue());
			i++;
		}
		graphs[0] = availabilityGraph;

		GraphData artGraph = new GraphData("Instant", "Average Response Time [ms]");
		values = instance.getAdaptationParamCollection().getValuesHistoryForParam(AverageResponseTime.class);
		valuesSize = values.size();
		oldestValueIndex = maxHistorySize > valuesSize ? valuesSize-1 : maxHistorySize-1;
		for (int i = 0; i <= oldestValueIndex; i++) { // get only latest X values
			AdaptationParameter.Value v = values.get(oldestValueIndex-i);
			artGraph.addPoint(v.getTimestamp().before(serviceLatestAdaptationDate) ? "B"+(i+1) : "A"+(i+1), v.getValue()*1000);
		}
		graphs[1] = artGraph;

		return graphs;
	}

}
