package it.polimi.ramses.plan.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "EXECUTE", url = "${EXECUTE_URL}")
public interface ExecuteClient {

    @GetMapping("/rest/start")
    String start();
}
