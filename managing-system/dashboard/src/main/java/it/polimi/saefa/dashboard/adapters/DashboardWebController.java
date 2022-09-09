package it.polimi.saefa.dashboard.adapters;

import it.polimi.saefa.dashboard.domain.DashboardWebService;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.Arrays;
import java.util.Map;

@Controller
@Slf4j
public class DashboardWebController {

	@Autowired 
	private DashboardWebService dashboardWebService;

	/* Mostra home page */
	@GetMapping("/{serviceId}/instances")
	public String instancesDetails(Model model, @PathVariable String serviceId) {
		//Service s = dashboardWebService.getAllServices().stream().filter(s -> s.getServiceId().equals(serviceId)).findFirst().orElse(null);
		Service service = dashboardWebService.getService(serviceId);
		log.info("Service: " + service);
		model.addAttribute("service", service);
		model.addAttribute("currentImplementation", service.getPossibleImplementations().get(service.getCurrentImplementation()));
		return "webpages/instancesDetails";
	}

	/* Mostra home page */
	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("architecture", dashboardWebService.getArchitecture().values().stream().toList());
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
		/*model.addAttribute("graphData", data);
		model.addAttribute("xAxisName", data.getXAxisName());
		model.addAttribute("yAxisName", data.getYAxisName());
		model.addAttribute("pointsList", data.getPoints());*/
		return "webpages/adaptation";
	}
}
