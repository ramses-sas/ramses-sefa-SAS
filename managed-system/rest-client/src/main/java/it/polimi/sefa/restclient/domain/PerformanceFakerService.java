package it.polimi.sefa.restclient.domain;

import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class PerformanceFakerService {
    @Value("${FAKE_SLOW_ORDERING}")
    private String fakeSlowOrderingStr;

    @Value("${FAKE_SLOW_ORDERING_1_SLEEP}")
    private Double fakeSlowOrdering1Sleep;
    @Value("${FAKE_SLOW_ORDERING_1_START}")
    private Integer fakeSlowOrdering1Start;
    @Value("${FAKE_SLOW_ORDERING_1_DURATION}")
    private Integer fakeSlowOrdering1Duration;
    @Value("${FAKE_SLOW_ORDERING_2_SLEEP}")
    private Double fakeSlowOrdering2Sleep;
    @Value("${FAKE_SLOW_ORDERING_2_START}")
    private Integer fakeSlowOrdering2Start;
    @Value("${FAKE_SLOW_ORDERING_2_DURATION}")
    private Integer fakeSlowOrdering2Duration;
    
    @Value("${FAKE_UNREACHABLE_RESTAURANT_COUNTER}")
    private Integer fakeUnreachableRestaurantCounter;
    @Value("${FAKE_UNREACHABLE_RESTAURANT_DELAY}")
    private Integer fakeUnreachableRestaurantDelay;
    
    @Autowired
    private AdaptationController adaptationController;
    @Autowired
    private EurekaClient discoveryClient;
    
    private Map<String, Double> originalInstancesSleeps;

    @PostConstruct
    public void init() {
        discoveryClient.getApplications(); // force eureka client to initialize
        boolean fakeSlowOrdering = fakeSlowOrderingStr.equalsIgnoreCase("Y");
        log.info("PerformanceFakerService initialized with the following values:");
        log.info("fakeSlowOrdering? {}", fakeSlowOrderingStr.equalsIgnoreCase("Y") ? "YES" : "NO");
        log.info("fakeSlowOrdering1Sleep: {}", fakeSlowOrdering1Sleep);
        log.info("fakeSlowOrdering1Start: {}", fakeSlowOrdering1Start);
        log.info("fakeSlowOrdering1Duration: {}", fakeSlowOrdering1Duration);
        log.info("fakeSlowOrdering2Sleep: {}", fakeSlowOrdering2Sleep);
        log.info("fakeSlowOrdering2Start: {}", fakeSlowOrdering2Start);
        log.info("fakeSlowOrdering2Duration: {}", fakeSlowOrdering2Duration);
        log.info("fakeUnreachableRestaurantCounter: {}", fakeUnreachableRestaurantCounter);
        log.info("fakeUnreachableRestaurantDelay: {}", fakeUnreachableRestaurantDelay);
        if (fakeSlowOrdering) {
            TimerTask fakeSlowOrderingTask1Start = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 1");
                    originalInstancesSleeps = getSleepForOrderingServiceInstances();
                    changeSleepForOrderingServiceInstances(fakeSlowOrdering1Sleep);
                }
            };
            Timer fakeSlowOrderingTimer1Start = new Timer("fakeSlowOrderingTimer1Start");
            fakeSlowOrderingTimer1Start.schedule(fakeSlowOrderingTask1Start, 1000L * fakeSlowOrdering1Start);
            
            TimerTask fakeSlowOrderingTask1End = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 1 end");
                    changeSleepForOrderingServiceInstances(0.0);
                }
            };
            Timer fakeSlowOrderingTimer1End = new Timer("fakeSlowOrderingTimer1End");
            fakeSlowOrderingTimer1End.schedule(fakeSlowOrderingTask1End, 1000L * (fakeSlowOrdering1Start+fakeSlowOrdering1Duration));
            
            TimerTask fakeSlowOrderingTask2Start = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 2");
                    originalInstancesSleeps = getSleepForOrderingServiceInstances();
                    changeSleepForOrderingServiceInstances(fakeSlowOrdering2Sleep);
                }
            };
            Timer fakeSlowOrderingTimer2Start = new Timer("fakeSlowOrderingTimer2Start");
            fakeSlowOrderingTimer2Start.schedule(fakeSlowOrderingTask2Start, 1000L * fakeSlowOrdering2Start);
            
            TimerTask fakeSlowOrderingTask2End = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 2 end");
                    changeSleepForOrderingServiceInstances(0.0);
                }
            };
            Timer fakeSlowOrderingTimer2End = new Timer("fakeSlowOrderingTimer2End");
            fakeSlowOrderingTimer2End.schedule(fakeSlowOrderingTask2End, 1000L * (fakeSlowOrdering2Start+fakeSlowOrdering2Duration));
        }

        if (fakeUnreachableRestaurantCounter != 0) {
            TimerTask fakeUnreachableRestaurantTask = new TimerTask() {
                public void run() {
                    log.info("Faking unreachable restaurant");
                    try {
                        adaptationController.setFakeCounter(fakeUnreachableRestaurantCounter);
                    } catch (Exception e) {
                        log.error("Error while faking unreachable restaurant", e);
                        System.exit(1);
                    }
                }
            };
            Timer fakeUnreachableRestaurantTimer = new Timer("fakeUnreachableRestaurantTimer");
            fakeUnreachableRestaurantTimer.schedule(fakeUnreachableRestaurantTask, 1000L * 60 * fakeUnreachableRestaurantDelay);
        }
    }

    private Map<String, Double> getSleepForOrderingServiceInstances() {
        Map<String, Double> instancesOriginalSleep = new HashMap<>();
        try {
            discoveryClient.getApplication("ORDERING-SERVICE").getInstances().forEach(instance -> {
                String url = "http://" + instance.getHostName() + ":" + instance.getPort() + "/rest/instrumentation/sleepMean";
                RestTemplate restTemplate = new RestTemplate();
                instancesOriginalSleep.put(instance.getInstanceId(), Double.parseDouble(Objects.requireNonNull(restTemplate.getForObject(url, String.class))));
                log.info("Initial sleep for instance {}: {}", instance.getInstanceId(), instancesOriginalSleep.get(instance.getInstanceId()));
            });
        } catch (Exception e) {
            log.error("Error while slowing down ordering", e);
            System.exit(1);
        }
        return instancesOriginalSleep;
    }
    
    private void changeSleepForOrderingServiceInstances(Double sleepMean) {
        try {
            discoveryClient.getApplication("ORDERING-SERVICE").getInstances().forEach(instance -> {
                Double originalSleep = originalInstancesSleeps.get(instance.getInstanceId());
                if (originalSleep == null) return;
                String url = "http://" + instance.getHostName() + ":" + instance.getPort() + "/rest/instrumentation/sleepMean?sleepMean={sleepMean}";
                RestTemplate restTemplate = new RestTemplate();
                log.info("Changing sleep for instance {} from {} to {}", instance.getInstanceId(), originalSleep, sleepMean+originalSleep);
                try {
                    restTemplate.put(url, null, Map.of("sleepMean", String.valueOf(originalSleep+sleepMean)));
                } catch (Exception e) {
                    if (sleepMean == 0.0) {
                        log.error("Error while restoring ordering. The instance {} could have been shutdown", instance.getInstanceId());
                    } else {
                        log.error("Error while slowing down ordering", e);
                        System.exit(1);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error while slowing down ordering", e);
            System.exit(1);
        }
    }
}
