package it.polimi.saefa.instancesmanager.rest;

import it.polimi.saefa.instancesmanager.domain.InstancesManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Array;

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
        instancesManagerService.removeInstance("payment-proxy-1-service", instancesManagerService.getDockerIp(), Integer.parseInt(dockerPort));
        return "OK";
    }
}
