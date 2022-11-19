package it.polimi.ramses.execute.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "MONITOR", url = "${MONITOR_URL}")
public interface MonitorClient {

    @GetMapping("/rest/notifyFinishedIteration")
    void notifyFinishedIteration();

}
