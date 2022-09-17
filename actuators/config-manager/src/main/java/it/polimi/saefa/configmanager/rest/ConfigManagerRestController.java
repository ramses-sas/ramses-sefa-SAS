package it.polimi.saefa.configmanager.rest;

import it.polimi.saefa.instancesmanager.domain.ServiceContainerInfo;
import it.polimi.saefa.instancesmanager.domain.ConfigManagerService;
import it.polimi.saefa.instancesmanager.restinterface.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class ConfigManagerRestController implements ConfigManagerRestInterface {

	@Autowired
	private ConfigManagerService configManagerService;

	@Override
	@PostMapping(path = "/addInstances")
	public AddInstancesResponse deliverOrder(@RequestBody AddInstancesRequest request) {
		AddInstancesResponse response = new AddInstancesResponse();
		List<ServiceContainerInfo> di = configManagerService.addInstances(request.getServiceImplementationName(), request.getNumberOfInstances());
		di.forEach(info -> response.addContainerInfo(info.getImageName(), info.getContainerId(), info.getContainerName(), info.getAddress(), info.getPort()));
		return response;
	}

	@Override
	@PostMapping(path = "/removeInstance")
	public RemoveInstanceResponse deliverOrder(@RequestBody RemoveInstanceRequest request) {
		configManagerService.removeInstance(request.getServiceImplementationName(), request.getAddress(), request.getPort());
		return new RemoveInstanceResponse(request.getServiceImplementationName(), request.getAddress(), request.getPort());
	}

}
