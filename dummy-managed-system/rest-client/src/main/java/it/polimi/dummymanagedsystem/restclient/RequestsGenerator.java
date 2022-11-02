package it.polimi.dummymanagedsystem.restclient;

import it.polimi.dummymanagedsystem.restclient.domain.AdaptationController;
import it.polimi.dummymanagedsystem.restclient.domain.RequestGeneratorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@Component
@Data
@EnableAsync
public class RequestsGenerator {
    @Value("${ADAPT}")
    private int adapt;
    @Value("${TRIAL_DURATION_MINUTES}")
    private long trialDurationMinutes;
    @Value("${spring.task.execution.pool.core-size}")
    private int poolSize;

    @Autowired
    private RequestGeneratorService requestGeneratorService;
    @Autowired
    private AdaptationController adaptationController;

    @PostConstruct
    public void init() {
        log.info("Adapt? {}", adapt != 0);
        log.info("Trial duration: {} minutes", trialDurationMinutes);
        log.info("Thread pool size: {}", poolSize);

        // After 10 seconds start Monitor. Then enable adaptation if ADAPT env var is != 0
        TimerTask startManagingTask = new TimerTask() {
            public void run() {
                log.info("Starting Monitor Routine");
                try {
                    adaptationController.startMonitorRoutine();
                    if (adapt != 0) {
                        log.info("Enabling adaptation");
                        adaptationController.changeAdaptationStatus(true);
                    } else {
                        log.info("Disabling adaptation");
                        adaptationController.changeAdaptationStatus(false);
                    }
                } catch (Exception e) {
                    log.error("Error while starting Monitor Routine", e);
                    System.exit(1);
                }
            }
        };
        Timer startManagingTimer = new Timer("StartManagingTimer");
        startManagingTimer.schedule(startManagingTask, 1000*10);

        // Stop simulation after TRIAL_DURATION_MINUTES minutes
        TimerTask stopSimulationTask = new TimerTask() {
            public void run() {
                try {
                    log.info("Stopping Monitor Routine");
                    adaptationController.stopMonitorRoutine();
                    log.info("Disabling adaptation");
                    adaptationController.changeAdaptationStatus(false);
                } catch (Exception e) {
                    log.error("Error while stopping simulation", e);
                    System.exit(1);
                }
                System.exit(0);
            }
        };
        Timer stopSimulationTimer = new Timer("StopSimulationTimer");
        stopSimulationTimer.schedule(stopSimulationTask, 1000*60*trialDurationMinutes);
    }


    @Async
    @Scheduled(fixedDelay = 10)
    public void scheduleFixedRateTaskAsync() {
        try {
            log.debug(String.valueOf(requestGeneratorService.getRandomNumber()));
        } catch (Exception e) {
            //log.error(e.getMessage());
        }
    }
}
