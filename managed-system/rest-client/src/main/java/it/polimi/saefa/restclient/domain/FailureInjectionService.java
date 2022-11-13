package it.polimi.saefa.restclient.domain;

import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Service
public class FailureInjectionService {
    @Value("${FAILURE_INJECTION}")
    private String failureInjectionStr;
    @Value("${FAILURE_INJECTION_1_START}")
    private Integer failureInjection1Start;
    @Value("${FAILURE_INJECTION_1_DURATION}")
    private Integer failureInjection1Duration;
    @Value("${FAILURE_INJECTION_2_START}")
    private Integer failureInjection2Start;
    @Value("${FAILURE_INJECTION_2_DURATION}")
    private Integer failureInjection2Duration;
    @Value("${ID_OF_INSTANCE_TO_FAIL}")
    private String idOfInstanceToFail;

    @Autowired
    private AdaptationController adaptationController;
    @Autowired
    private EurekaClient discoveryClient;

    @PostConstruct
    public void init() {
        discoveryClient.getApplications(); // force eureka client to initialize
        boolean injectFailure = failureInjectionStr.equalsIgnoreCase("Y");
        log.info("FailureInjectionService initialized with the following values:");
        log.info("injectFailure? {}", failureInjectionStr.equalsIgnoreCase("Y") ? "YES" : "NO");
        log.info("failureInjection1Start: {}", failureInjection1Start);
        log.info("failureInjection1Duration: {}", failureInjection1Duration);
        log.info("failureInjection2Start: {}", failureInjection2Start);
        log.info("failureInjection2Duration: {}", failureInjection2Duration);
        log.info("idOfInstanceToFail: {}", idOfInstanceToFail);

        // DOPO failureInjection1Start MINUTI chiedi di simulare failureInjection1Duration comportamenti anomali
        if (injectFailure) {
            Timer timer = new Timer();
            TimerTask failureInjection1StartTask = new TimerTask() {
                @Override
                public void run() {
                    log.info("failureInjection1Start: injecting failure");
                    adaptationController.stopInstance(idOfInstanceToFail);
                }
            };
            TimerTask failureInjection1StopTask = new TimerTask() {
                @Override
                public void run() {
                    log.info("failureInjection1Stop: restoring instance after failure injection");
                    adaptationController.startInstance(idOfInstanceToFail);
                }
            };
            TimerTask failureInjection2StartTask = new TimerTask() {
                @Override
                public void run() {
                    log.info("failureInjection2Start: injecting failure");
                    adaptationController.stopInstance(idOfInstanceToFail);
                }
            };
            TimerTask failureInjection2StopTask = new TimerTask() {
                @Override
                public void run() {
                    log.info("failureInjection2Stop: restoring instance after failure injection");
                    adaptationController.startInstance(idOfInstanceToFail);
                }
            };
            timer.schedule(failureInjection1StartTask, failureInjection1Start * 1000L);
            if (failureInjection1Duration != 0)
                timer.schedule(failureInjection1StopTask, (failureInjection1Start+failureInjection1Duration) * 1000L);
            /*
            timer.schedule(failureInjection2StartTask, failureInjection2Start * 1000L);
            if (failureInjection2Duration != 0)
                timer.schedule(failureInjection2StopTask, (failureInjection2Start+failureInjection2Duration) * 1000L);
             */
        }
    }

}
