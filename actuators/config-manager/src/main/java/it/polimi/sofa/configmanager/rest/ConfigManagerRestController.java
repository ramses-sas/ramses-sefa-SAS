package it.polimi.sofa.configmanager.rest;

import it.polimi.sofa.configmanager.domain.ConfigManagerService;
import it.polimi.sofa.configmanager.restinterface.ChangeLBWeightsRequest;
import it.polimi.sofa.configmanager.restinterface.ChangePropertyRequest;
import it.polimi.sofa.configmanager.restinterface.PropertyToChange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(path="/rest")
public class ConfigManagerRestController {

	@Autowired
	private ConfigManagerService configManagerService;

	@PostMapping(path = "/changeLBWeights")
	public void changeLBWeights(@RequestBody ChangeLBWeightsRequest request) {
		try {
			configManagerService.pull();
			configManagerService.updateLoadbalancerWeights(request.getServiceId(), request.getNewWeights(), request.getInstancesToRemoveWeightOf());
			configManagerService.commitAndPush("ConfigManagerActuator: changing properties");
		} catch (Exception e) {
			try {
				configManagerService.rollback();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			throw new RuntimeException(e);
		}
	}


	@PostMapping(path = "/changeProperty")
	public void changeProperty(@RequestBody ChangePropertyRequest request) {
		String filename;
		try {
			configManagerService.pull();
			for (PropertyToChange propertyToChange : request.getPropertiesToChange()) {
				filename = propertyToChange.getServiceName() == null ? "application.properties" : propertyToChange.getServiceName().toLowerCase() + ".properties";
				configManagerService.changePropertyInFile(propertyToChange.getPropertyName(), propertyToChange.getValue(), filename);
			}
			configManagerService.commitAndPush("ConfigManagerActuator: changing properties");
		} catch (Exception e) {
			try {
				configManagerService.rollback();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			throw new RuntimeException(e);
		}
	}

}
