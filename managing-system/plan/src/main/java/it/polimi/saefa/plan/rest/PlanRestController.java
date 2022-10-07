package it.polimi.saefa.plan.rest;

import it.polimi.saefa.plan.domain.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class PlanRestController {

    @Autowired
    private PlanService planService;

    @GetMapping(path="/start")
    public String start() {
        new Thread(() -> planService.startPlan()).start();
        return "OK";
    }

    // TODO remove after test
    @GetMapping("/")
    public String debug() {
        planService.breakpoint();
        return "Hello from Plan Service";
    }

}
