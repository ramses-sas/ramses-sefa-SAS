package it.polimi.saefa.probe.rest;

import it.polimi.saefa.probe.configuration.ServiceConfiguration;
import it.polimi.saefa.probe.domain.InstanceStatus;
import it.polimi.saefa.probe.domain.ProbeService;
import it.polimi.saefa.probe.domain.ServiceInfo;
import it.polimi.saefa.probe.domain.metrics.HttpEndpointMetrics;
import it.polimi.saefa.probe.domain.metrics.InstanceMetricsSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(path="/rest")
public class ProbeRestController {
    @Autowired
    ProbeService probeService;

    @Value("${FAKE_SLOW_ORDERING}")
    private String fakeSlowOrdering;

    @Value("${FAKE_UNREACHABLE_RESTAURANT}")
    private String fakeUnreachableRestaurant;


    private final Object lock = new Object();
    private Integer fakeCounter = 0;
    private String instanceToMakeUnreachable = null;
    private Map<String, InstanceMetricsSnapshot> latestInstancesSnapshot = new HashMap<>();

    @GetMapping("/service/{serviceId}/snapshot")
    public List<InstanceMetricsSnapshot> takeSnapshot(@PathVariable("serviceId") String serviceId) {
        List<InstanceMetricsSnapshot> snapshots = probeService.createServiceSnapshot(serviceId);
        if (fakeSlowOrdering.equalsIgnoreCase("Y") && snapshots != null && !snapshots.isEmpty() && serviceId.equalsIgnoreCase("ordering-service")) {
            int counter;
            synchronized (lock) {
                counter = fakeCounter;
                fakeCounter--;
            }
            if (counter > 0) {
                if (latestInstancesSnapshot.isEmpty()) {
                    snapshots.forEach(s -> latestInstancesSnapshot.put(s.getInstanceId(), s));
                } else {
                    log.info("Faking slow ordering service. Counter: {}", counter);
                    for (InstanceMetricsSnapshot snapshot : snapshots) {
                        InstanceMetricsSnapshot previousSnapshot = latestInstancesSnapshot.get(snapshot.getInstanceId());
                        if (previousSnapshot != null) {
                            previousSnapshot.getHttpMetrics().forEach((key, previousHttpMetrics) -> {
                                HttpEndpointMetrics currentHttpMetrics = snapshot.getHttpMetrics().get(key);
                                int previousCountOfSuccessful = previousHttpMetrics.getTotalCountOfSuccessful();
                                double previousDurationOfSuccessful = previousHttpMetrics.getTotalDurationOfSuccessful();
                                int currentCountOfSuccessful = currentHttpMetrics.getTotalCountOfSuccessful();
                                double currentDurationOfSuccessful = currentHttpMetrics.getTotalDurationOfSuccessful();
                                if (currentCountOfSuccessful > previousCountOfSuccessful) {
                                    currentHttpMetrics.getOutcomeMetrics().get("SUCCESS").setTotalDuration(currentDurationOfSuccessful + 2000 * (currentCountOfSuccessful - previousCountOfSuccessful));
                                }
                            });
                        }
                        latestInstancesSnapshot.put(snapshot.getInstanceId(), snapshot);
                    }
                }
            } else {
                latestInstancesSnapshot = new HashMap<>();
            }
        }
        if (fakeUnreachableRestaurant.equalsIgnoreCase("Y") && snapshots != null && !snapshots.isEmpty() && serviceId.equalsIgnoreCase("restaurant-service")) {
            synchronized (lock) {
                if (fakeCounter > 0) {
                    if (instanceToMakeUnreachable == null) {
                        instanceToMakeUnreachable = snapshots.get(0).getInstanceId();
                        snapshots.get(0).setStatus(InstanceStatus.UNREACHABLE);
                    } else {
                        snapshots.stream().filter(snapshot -> snapshot.getInstanceId().equalsIgnoreCase(instanceToMakeUnreachable)).forEach(snapshot -> {
                            snapshot.setStatus(InstanceStatus.UNREACHABLE);
                        });
                    }
                    log.info("Faking unreachable restaurant service. Instance: {}", instanceToMakeUnreachable);
                    fakeCounter--;
                }
            }
        }
        return snapshots;
    }

    @GetMapping("/systemArchitecture")
    public Map<String, ServiceInfo> getSystemArchitecture() {
        return probeService.getServices();
    }

    @GetMapping("/service/{serviceId}/configuration")
    public ServiceConfiguration getServiceConfiguration(@PathVariable("serviceId") String serviceId, @RequestParam("implementationId") String implementationId) {
        return probeService.getServiceConfiguration(serviceId, implementationId);
    }

    @PutMapping("/fakeCounter")
    public void setFakeCounter(@RequestParam("value") Integer value) {
        synchronized (lock) {
            fakeCounter = value;
        }
    }

}
