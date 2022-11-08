package it.polimi.saefa.restclient.domain;

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
    @Value("${FAKE_SLOW_ORDERING_SLEEP}")
    private Double fakeSlowOrderingSleep;
    @Value("${FAKE_SLOW_ORDERING_DELAY_1_START}")
    private Integer fakeSlowOrderingDelay1Start;
    @Value("${FAKE_SLOW_ORDERING_DELAY_1_END}")
    private Integer fakeSlowOrderingDelay1End;
    @Value("${FAKE_SLOW_ORDERING_DELAY_2_START}")
    private Integer fakeSlowOrderingDelay2Start;
    @Value("${FAKE_SLOW_ORDERING_DELAY_2_END}")
    private Integer fakeSlowOrderingDelay2End;
    
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
        boolean fakeSlowOrdering = fakeSlowOrderingStr.equalsIgnoreCase("Y");
        log.info("PerformanceFakerService initialized with the following values:");
        log.info("fakeSlowOrdering? {}", fakeSlowOrderingStr.equalsIgnoreCase("Y") ? "YES" : "NO");
        log.info("fakeSlowOrderingDelay1Start: {}", fakeSlowOrderingDelay1Start);
        log.info("fakeSlowOrderingDelay1End: {}", fakeSlowOrderingDelay1End);
        log.info("fakeSlowOrderingDelay2Start: {}", fakeSlowOrderingDelay2Start);
        log.info("fakeSlowOrderingDelay2End: {}", fakeSlowOrderingDelay2End);
        log.info("fakeUnreachableRestaurantCounter: {}", fakeUnreachableRestaurantCounter);
        log.info("fakeUnreachableRestaurantDelay: {}", fakeUnreachableRestaurantDelay);
        // DOPO fakeSlowOrderingDelay1 MINUTI chiedi di simulare COUNTER comportamenti anomali
        if (fakeSlowOrdering) {
            TimerTask fakeSlowOrderingTask1Start = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 1");
                    originalInstancesSleeps = getSleepForOrderingServiceInstances();
                    changeSleepForOrderingServiceInstances(fakeSlowOrderingSleep);
                }
            };
            Timer fakeSlowOrderingTimer1Start = new Timer("fakeSlowOrderingTimer1Start");
            fakeSlowOrderingTimer1Start.schedule(fakeSlowOrderingTask1Start, 1000L * 60 * fakeSlowOrderingDelay1Start);
            
            TimerTask fakeSlowOrderingTask1End = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 1 end");
                    changeSleepForOrderingServiceInstances(0.0);
                }
            };
            Timer fakeSlowOrderingTimer1End = new Timer("fakeSlowOrderingTimer1End");
            fakeSlowOrderingTimer1End.schedule(fakeSlowOrderingTask1End, 1000L * 60 * fakeSlowOrderingDelay1End);
            
            TimerTask fakeSlowOrderingTask2Start = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 2");
                    originalInstancesSleeps = getSleepForOrderingServiceInstances();
                    changeSleepForOrderingServiceInstances(fakeSlowOrderingSleep);
                }
            };
            Timer fakeSlowOrderingTimer2Start = new Timer("fakeSlowOrderingTimer2Start");
            fakeSlowOrderingTimer2Start.schedule(fakeSlowOrderingTask2Start, 1000L * 60 * fakeSlowOrderingDelay2Start);
            
            TimerTask fakeSlowOrderingTask2End = new TimerTask() {
                public void run() {
                    log.info("Faking slow ordering 2 end");
                    changeSleepForOrderingServiceInstances(0.0);
                }
            };
            Timer fakeSlowOrderingTimer2End = new Timer("fakeSlowOrderingTimer2End");
            fakeSlowOrderingTimer2End.schedule(fakeSlowOrderingTask2End, 1000L * 60 * fakeSlowOrderingDelay2End);
        }


        // DOPO fakeUnreachableRestaurantDelay MINUTI chiedi di simulare COUNTER metriche unreachable
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
                String url = "http://" + instance.getHostName() + ":" + instance.getPort() + "/rest/instrumentation/sleepMean?sleepMean={sleepMean}";
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.put(url, null, Map.of("sleepMean", String.valueOf(originalSleep+sleepMean)));
            });
        } catch (Exception e) {
            log.error("Error while slowing down ordering", e);
            System.exit(1);
        }
    }
}
