package it.polimi.saefa.instancesmanager.rest;

import it.polimi.saefa.instancesmanager.domain.InstancesManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {
    @Autowired
    private InstancesManagerService instancesManagerService;

    @GetMapping("/{echovar}")
    public String dummy(@PathVariable String echovar) {
        return echovar;
    }

    @GetMapping("/createContainer")
    public String createContainer() {
        instancesManagerService.addInstances("payment-proxy-1-service", 1);
        return "OK";
    }
}
