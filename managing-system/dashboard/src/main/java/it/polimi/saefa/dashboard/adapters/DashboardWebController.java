package it.polimi.saefa.dashboard.adapters;

import it.polimi.saefa.dashboard.domain.DashboardWebService;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

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

	/* Trova tutti i ristoranti. */
	@GetMapping("/adaptation")
	public String home(Model model) {
		return "webpages/adaptation";
	}
}
