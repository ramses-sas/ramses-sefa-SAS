package it.polimi.saefa.analyse.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "PLAN", url = "${PLAN_URL}")
public interface PlanClient {

    @GetMapping(path="/rest/start")
    String start();
}
