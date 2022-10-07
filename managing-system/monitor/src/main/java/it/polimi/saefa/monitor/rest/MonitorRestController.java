package it.polimi.saefa.monitor.rest;

import it.polimi.saefa.monitor.domain.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest")
public class MonitorRestController {
    @Autowired
    private MonitorService monitorService;

    @GetMapping("/notifyFinishedIteration")
    public void notifyFinishedIteration() {
        monitorService.setLoopIterationFinished(true);
    }

    // TODO remove after test
    @GetMapping("/")
    public String debug() {
        monitorService.breakpoint();
        return "Hello from Monitor Service";
    }
}
