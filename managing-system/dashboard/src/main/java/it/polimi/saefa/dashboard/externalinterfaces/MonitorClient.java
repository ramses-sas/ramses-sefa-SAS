package it.polimi.saefa.dashboard.externalinterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "MONITOR", url = "${MONITOR_URL}")
public interface MonitorClient {

    @GetMapping("/rest/schedulingPeriod")
    int getSchedulingPeriod();

    @PutMapping("/rest/schedulingPeriod")
    void changeSchedulingPeriod(@RequestParam("period") int period);

}