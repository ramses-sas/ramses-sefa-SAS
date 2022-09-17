package it.polimi.saefa.configmanager.rest;

import it.polimi.saefa.instancesmanager.domain.ConfigManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {
    @Autowired
    private ConfigManagerService configManagerService;

    @GetMapping("/")
    public String dummy() {
        return "Hi!";
    }

}
