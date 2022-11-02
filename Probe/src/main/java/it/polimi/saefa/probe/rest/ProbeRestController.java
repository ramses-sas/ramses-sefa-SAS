package it.polimi.saefa.probe.rest;

import it.polimi.saefa.probe.configuration.ServiceConfiguration;
import it.polimi.saefa.probe.domain.ProbeService;
import it.polimi.saefa.probe.domain.metrics.InstanceMetricsSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path="/rest")
public class ProbeRestController {
    @Autowired
    ProbeService probeService;

    @GetMapping("/service/{serviceId}/snapshot")
    public List<InstanceMetricsSnapshot> takeSnapshot(@PathVariable("serviceId") String serviceId) {
        return probeService.createServiceSnapshot(serviceId);
    }

    @GetMapping("/systemArchitecture")
    public SystemArchitectureResponse getSystemArchitecture() {
        return new SystemArchitectureResponse(probeService.getServices());
    }

    @GetMapping("/service/{serviceId}/configuration")
    public ServiceConfiguration getServiceConfiguration(@PathVariable("serviceId") String serviceId, @RequestParam("implementationId") String implementationId) {
        return probeService.getServiceConfiguration(serviceId, implementationId);
    }

}
