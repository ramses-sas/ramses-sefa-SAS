package it.polimi.saefa.dashboard.externalinterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "PLAN", url = "${PLAN_URL}")
public interface PlanClient {

    @GetMapping("/rest/adaptationStatus")
    String getAdaptationStatus();

    @PutMapping("/rest/adaptationStatus")
    String setAdaptationStatus(@RequestParam boolean adapt);
}

