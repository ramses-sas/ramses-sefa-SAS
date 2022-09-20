package it.polimi.saefa.dashboard.adapters;

import it.polimi.saefa.dashboard.domain.DashboardWebService;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.parameters.AdaptationParameter;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import it.polimi.saefa.knowledge.persistence.domain.metrics.CircuitBreakerMetrics;
import it.polimi.saefa.knowledge.persistence.domain.metrics.HttpRequestMetrics;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@Slf4j
public class DashboardWebController {

	@Autowired 
	private DashboardWebService dashboardWebService;

	@GetMapping("/service/{serviceId}")
	public String serviceDetails(Model model, @PathVariable String serviceId) {
		Service service = dashboardWebService.getService(serviceId);
		log.info("Service: " + service + " for serviceId: " + serviceId);
		Set<String> possibleImplementations = service.getPossibleImplementations().keySet();
		// [[InstanceId, Status, LatestMetricsDescription]]
		List<String[]> instancesTable = new ArrayList<>();
		for (Instance instance : service.getInstances()) {
			instancesTable.add(new String[]{instance.getInstanceId(), instance.getCurrentStatus().toString()});
		}
		//model.addAttribute("service", service);
		model.addAttribute("serviceId", serviceId);
		//model.addAttribute("currentImplementationTable", currentImplementationTable);
		model.addAttribute("possibleImplementations", possibleImplementations);
		model.addAttribute("instancesTable", instancesTable);
		//model.addAttribute("currentImplementation", service.getPossibleImplementations().get(service.getCurrentImplementation()));

		GraphData[] graphs = new GraphData[3];
		GraphData data = new GraphData("Instant", "Availability");
		for (int i = 0; i < 20; i++) {
			data.addPoint("" + i, i * Math.random());
		}
		graphs[0] = data;
		GraphData data1 = new GraphData("Instant", "Average Response Time");
		for (int i = 0; i < 20; i++) {
			data1.addPoint("" + i, i * Math.random());
		}
		graphs[1] = data1;
		GraphData data2 = new GraphData("Instant", "Max Response Time");
		for (int i = 0; i < 20; i++) {
			data2.addPoint("" + i, i * Math.random());
		}
		graphs[2] = data2;
		log.debug("Graphs: " + Arrays.toString(graphs));
		model.addAttribute("graphs", graphs);

		return "webpages/serviceDetails";
	}

	@GetMapping("/service/{serviceId}/{instanceId}/metrics")
	public String instanceMetrics(Model model, @PathVariable String serviceId, @PathVariable String instanceId) {
		InstanceMetrics latestMetrics = dashboardWebService.getLatestMetrics(serviceId, instanceId);
		log.debug("Latest metrics: " + latestMetrics);
		List<String[]> resourceTable = new ArrayList<>();
		List<String[]> httpMetricsTable = new ArrayList<>();
		List<String[]> circuitBreakersTable = new ArrayList<>();
		if (latestMetrics != null) {
			resourceTable.add(new String[]{"CPU Usage", "" + String.format("%.2f", latestMetrics.getCpuUsage()*100)+"%"});
			resourceTable.add(new String[]{"Disk Free Space", String.format("%.2f", latestMetrics.getDiskFreeSpace()/1024/1024/1024)+" GB"});
			resourceTable.add(new String[]{"Disk Total Space", String.format("%.2f", latestMetrics.getDiskTotalSpace()/1024/1024/1024)+" GB"});
			for (HttpRequestMetrics httpMetrics : latestMetrics.getHttpMetrics().values())
				for(String outcome : httpMetrics.getOutcomes())
					httpMetricsTable.add(new String[]{httpMetrics.getHttpMethod() + " " + httpMetrics.getEndpoint(),
						outcome, httpMetrics.getAverageDurationByOutcome(outcome) == -1 ? "N/A" : String.format("%.2f", httpMetrics.getAverageDurationByOutcome(outcome))+" s"});
			for (CircuitBreakerMetrics cbMetrics : latestMetrics.getCircuitBreakerMetrics().values()) {
				circuitBreakersTable.add(new String[]{"Circuit Breaker Name", cbMetrics.getName()});
				double failureRate = cbMetrics.getFailureRate();
				circuitBreakersTable.add(new String[]{"Failure Rate", failureRate==-1 ? "N/A" : String.format("%.1f", failureRate*100)+"%"});
				circuitBreakersTable.add(new String[]{"Failed Calls Count", cbMetrics.getNotPermittedCallsCount()+""});
				double slowCallRate = cbMetrics.getSlowCallRate();
				circuitBreakersTable.add(new String[]{"Slow Calls Rate", slowCallRate==-1 ? "N/A" : String.format("%.1f", slowCallRate*100)+"%"});
				circuitBreakersTable.add(new String[]{"Slow Calls Count", cbMetrics.getSlowCallCount()+""});
				for (CircuitBreakerMetrics.CallOutcomeStatus status : CircuitBreakerMetrics.CallOutcomeStatus.values()) {
					Double avgDuration = cbMetrics.getAverageDuration(status);
					circuitBreakersTable.add(new String[]{"Average Call Duration when "+status, avgDuration.isNaN() ? "N/A" : String.format("%.2f", avgDuration)+" s"});
				}
				circuitBreakersTable.add(new String[]{"", ""});
			}
		}
		model.addAttribute("resourceTable", resourceTable);
		model.addAttribute("httpMetricsTable", httpMetricsTable);
		model.addAttribute("circuitBreakersTable", circuitBreakersTable);
		return "webpages/instanceMetrics";
	}

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
			// List <ConfigPropertyName, Value>
			List<String[]> table = new ArrayList<>();
			table.add(new String[]{"Time Of Snapshot", sdf.format(conf.getTimestamp())+" UTC"});
			table.add(new String[]{"", ""});
			table.add(new String[]{"Load Balancer Type", conf.getLoadBalancerType().replace("_", " ")});
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

			// List <ParameterName, Value, Threshold, Weight, Priority>
			List<String[]> serviceAdaptationParametersTable = new ArrayList<>();
			for (AdaptationParameter ap : s.getAdaptationParameters()) {
				serviceAdaptationParametersTable.add(new String[]{ap.getClass().getSimpleName(), ap.getValue().toString(), ap.getThreshold().toString(), ap.getWeight().toString(), String.valueOf(ap.getPriority())});
			}
			servicesAdaptationParametersTable.put(s.getServiceId(), serviceAdaptationParametersTable);

			ServiceImplementation currentImplementation = s.getPossibleImplementations().get(s.getCurrentImplementation());
			// [[ImplementationName, CostPerBoot, CostPerInstance, ...]]
			List<String[]> currentImplementationTable = new ArrayList<>();
			log.info(Arrays.toString(currentImplementation.getClass().getDeclaredFields()));
			currentImplementationTable.add(new String[]{"Implementation Name", currentImplementation.getImplementationId()});
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

	/* Plot adaptation parameters. */
	@GetMapping("/adaptation")
	public String home(Model model) {
		GraphData[] graphs = new GraphData[3];
		GraphData data = new GraphData("Instant", "Availability");
		for (int i = 0; i < 20; i++) {
			data.addPoint("" + i, i * Math.random());
		}
		graphs[0] = data;
		GraphData data1 = new GraphData("Instant", "Average Response Time");
		for (int i = 0; i < 20; i++) {
			data1.addPoint("" + i, i * Math.random());
		}
		graphs[1] = data1;
		GraphData data2 = new GraphData("Instant", "Max Response Time");
		for (int i = 0; i < 20; i++) {
			data2.addPoint("" + i, i * Math.random());
		}
		graphs[2] = data2;
		log.debug("Graphs: " + Arrays.toString(graphs));
		model.addAttribute("graphs", graphs);
		return "webpages/adaptation";
	}
}
