package it.polimi.saefa.configmanager.rest;

import it.polimi.saefa.configmanager.domain.ConfigManagerService;
import it.polimi.saefa.configmanager.restinterface.AddOrUpdatePropertyRequest;
import it.polimi.saefa.configmanager.restinterface.ConfigManagerRestInterface;
import it.polimi.saefa.configmanager.restinterface.RemovePropertyRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(path="/rest")
public class ConfigManagerRestController implements ConfigManagerRestInterface {

	@Autowired
	private ConfigManagerService configManagerService;

	@PostMapping(path = "/addOrUpdateProperty")
	public void addOrUpdateProperty(@RequestBody AddOrUpdatePropertyRequest request) {
		String filename = request.getServiceName() == null ? "application.properties" : request.getServiceName().toLowerCase() + ".properties";
		try {
			configManagerService.addOrUpdatePropertyAndPush(request.getPropertyName(), request.getValue(), filename);
		} catch (Exception e) {
			throw new RuntimeException("Failed to add or update property "+request.getPropertyName()
					+" with value "+request.getValue()+" for service "+request.getServiceName(), e);
		}
	}

	@PostMapping(path = "/removeProperty")
	public void removeProperty(@RequestBody RemovePropertyRequest request) {
		String filename = request.getServiceName() == null ? "application.properties" : request.getServiceName().toLowerCase() + ".properties";
		try {
			configManagerService.removePropertyAndPush(request.getPropertyName(), filename);
		} catch (Exception e) {
			throw new RuntimeException("Failed to remove property "+request.getPropertyName()
					+" for service "+request.getServiceName(), e);
		}
	}

}
