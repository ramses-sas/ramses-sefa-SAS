package it.polimi.sefa.instancesmanager.rest;

import it.polimi.sefa.instancesmanager.restinterface.AddInstancesResponse;
import it.polimi.sefa.instancesmanager.restinterface.AddInstancesTESTRequest;
import it.polimi.sefa.instancesmanager.domain.InstancesManagerService;
import it.polimi.sefa.instancesmanager.domain.ServiceContainerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {
    @Autowired
    private InstancesManagerService instancesManagerService;

    @GetMapping("/")
    public String dummy() {
        return "Hi!";
    }

    @GetMapping("/createContainer")
    public String createContainer() {
        return instancesManagerService.addInstances("payment-proxy-1-service", 1).toString();
    }

    @GetMapping("/removeContainer/{dockerPort}")
    public String removeContainer(@PathVariable("dockerPort") String dockerPort) {
        instancesManagerService.stopInstance("payment-proxy-1-service", Integer.parseInt(dockerPort));
        return "OK";
    }

    @PostMapping(path = "/addInstances")
    public AddInstancesResponse addInstances(@RequestBody AddInstancesTESTRequest request) {
        AddInstancesResponse response = new AddInstancesResponse();
        List<ServiceContainerInfo> di = instancesManagerService.addInstances(request.getServiceImplementationName(), request.getNumberOfInstances(), request.getExceptionRate(), request.getSleepDuration(), request.getSleepVariance());
        di.forEach(info -> response.addContainerInfo(info.getImageName(), info.getContainerId(), info.getContainerName(), info.getAddress(), info.getPort(), info.getEnvVars()));
        return response;
    }
}
