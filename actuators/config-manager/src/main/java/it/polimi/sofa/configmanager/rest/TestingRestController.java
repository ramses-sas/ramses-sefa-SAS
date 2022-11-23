package it.polimi.sofa.configmanager.rest;

import it.polimi.sofa.configmanager.domain.ConfigManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest/test")
public class TestingRestController {
    @Autowired
    private ConfigManagerService configManagerService;

    @GetMapping("/add")
    public String add() {
        try {
            configManagerService.pull();
            configManagerService.changeProperty("test", "test", "application.properties");
            configManagerService.commitAndPush("ConfigManagerActuator: changing properties");
        } catch (Exception e) {
            return "FAILED!\n"+e.getMessage();
        }
        return "Done!";
    }

    @GetMapping("/remove")
    public String remove() {
        try {
            configManagerService.pull();
            configManagerService.changeProperty("test", null, "application.properties");
            configManagerService.commitAndPush("ConfigManagerActuator: changing properties");
        } catch (Exception e) {
            return "FAILED!\n"+e.getMessage();
        }
        return "Done!";
    }

}
