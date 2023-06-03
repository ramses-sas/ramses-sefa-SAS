package it.polimi.ramses.plan.rest;

import it.polimi.ramses.plan.domain.PlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/adaptationStatus")
    public String getAdaptationStatus() {
        return String.valueOf(planService.isAdaptationAuthorized());
    }

    @PutMapping("/adaptationStatus")
    public String setAdaptationStatus(@RequestParam boolean adapt) {
        log.info("Setting adaptation status to {}", adapt ? "ENABLED" : "DISABLED");
        planService.setAdaptationAuthorized(adapt);
        return "OK";
    }
}
