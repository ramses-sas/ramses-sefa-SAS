package it.polimi.sofa.instancesmanager.rest;

import it.polimi.sofa.instancesmanager.domain.ServiceContainerInfo;
import it.polimi.sofa.instancesmanager.domain.InstancesManagerService;
import it.polimi.sofa.instancesmanager.restinterface.*;
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
public class InstancesManagerRestController {

	@Autowired
	private InstancesManagerService instancesManagerService;

	@PostMapping(path = "/addInstances")
	public AddInstancesResponse addInstances(@RequestBody AddInstancesRequest request) {
		AddInstancesResponse response = new AddInstancesResponse();
		List<ServiceContainerInfo> di = instancesManagerService.addInstances(request.getServiceImplementationName(), request.getNumberOfInstances());
		di.forEach(info -> response.addContainerInfo(info.getImageName(), info.getContainerId(), info.getContainerName(), info.getAddress(), info.getPort(), info.getEnvVars()));
		return response;
	}

	@PostMapping(path = "/startInstance")
	public StartInstanceResponse startInstance(@RequestBody StartInstanceRequest request) {
		instancesManagerService.startInstance(request.getServiceImplementationName(), request.getPort());
		return new StartInstanceResponse(request.getServiceImplementationName(), request.getAddress(), request.getPort());
	}

	@PostMapping(path = "/removeInstance")
	public RemoveInstanceResponse removeInstance(@RequestBody RemoveInstanceRequest request) {
		instancesManagerService.stopInstance(request.getServiceImplementationName(), request.getPort());
		return new RemoveInstanceResponse(request.getServiceImplementationName(), request.getAddress(), request.getPort());
	}

	@PostMapping(path = "/changeProfile")
	public String changeProfile(@RequestBody String newProfile) {
		instancesManagerService.changeProfile(newProfile);
		return "Profile changed to " + newProfile;
	}

}
