package it.polimi.saefa.monitor.domain;

import com.netflix.appinfo.InstanceInfo;
import it.polimi.saefa.knowledge.domain.Modules;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.saefa.monitor.InstancesSupplier;
import it.polimi.saefa.monitor.externalinterfaces.AnalyseClient;
import it.polimi.saefa.monitor.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.monitor.prometheus.PrometheusParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@EnableScheduling
public class MonitorService {
    private final Set<String> managedServices = new HashSet<>();

    private final KnowledgeClient knowledgeClient;

    ThreadPoolTaskScheduler taskScheduler;
    @Getter
    private ScheduledFuture<?> monitorRoutine;

    @Autowired
    private AnalyseClient analyseClient;
    @Autowired
    private InstancesSupplier instancesSupplier;
    @Autowired
    private PrometheusParser prometheusParser;

    @Value("${INTERNET_CONNECTION_CHECK_HOST}")
    private String internetConnectionCheckHost;
    @Value("${INTERNET_CONNECTION_CHECK_PORT}")
    private int internetConnectionCheckPort;

    @Getter
    @Value("${SCHEDULING_PERIOD}")
    private int schedulingPeriod = 5000; // monitor scheduling period [ms]

    private final AtomicBoolean loopIterationFinished = new AtomicBoolean(true);
    private final Queue<List<InstanceMetricsSnapshot>> instanceMetricsListBuffer = new LinkedList<>(); //linkedlist is FIFO

    public MonitorService(KnowledgeClient knowledgeClient, ThreadPoolTaskScheduler taskScheduler) {
        this.knowledgeClient = knowledgeClient;
        this.taskScheduler = taskScheduler;
        knowledgeClient.getServices().forEach(service -> managedServices.add(service.getServiceId()));
    }

    /*
    // TODO decomment to start routine on startup
    @PostConstruct
    public void initRoutine() {
        monitorRoutine = taskScheduler.scheduleWithFixedDelay(new MonitorRoutine(), schedulingPeriod);
    }
     */

    // ASSUNZIONE: IL PERIODO DEVE ESSERE "ABBASTANZA" MINORE DEL PERIODO DI AGGIORNAMENTO DEL DISCOVERY SERVICE (PER EUREKA DI DEFAULT 90SEC)
    // ASSUNZIONE CHE QUANDO UN'ISTANZA è SU EUREKA HA TERMINATO IL PROCESSO DI STARTUP (ERGO NON C'è INIT DOPO LA REGISTRAZIONE A EUREKA)
    //@Scheduled(fixedDelayString = "#{monitorConfig.period}") //delay in milliseconds
    class MonitorRoutine implements Runnable {
        @Override
        public void run() {
            try {
                log.debug("\nA new Monitor routine iteration started");
                Map<String, List<InstanceInfo>> services = instancesSupplier.getServicesInstances();
                log.debug("SERVICES: " + services);
                List<InstanceMetricsSnapshot> metricsList = Collections.synchronizedList(new LinkedList<>());
                List<Thread> threads = new LinkedList<>();
                AtomicBoolean invalidIteration = new AtomicBoolean(false);

                services.forEach((serviceId, serviceInstances) -> {
                    if (managedServices.contains(serviceId)) {
                        serviceInstances.forEach(instance -> {
                            Thread thread = new Thread(() -> {
                                InstanceMetricsSnapshot instanceMetricsSnapshot;
                                try {
                                    instanceMetricsSnapshot = prometheusParser.parse(instance);
                                    instanceMetricsSnapshot.applyTimestamp();
                                    log.debug("Adding metric for instance {}", instanceMetricsSnapshot.getInstanceId());
                                    metricsList.add(instanceMetricsSnapshot);
                                } catch (Exception e) {
                                    log.error("Error adding metrics for {}. Considering it as unreachable", instance.getInstanceId());
                                    log.error("The exception is: " + e.getMessage());
                                    instanceMetricsSnapshot = new InstanceMetricsSnapshot(instance.getAppName(), instance.getInstanceId());
                                    instanceMetricsSnapshot.setStatus(InstanceStatus.UNREACHABLE);
                                    instanceMetricsSnapshot.applyTimestamp();
                                    metricsList.add(instanceMetricsSnapshot);
                                    try {
                                        if (!pingHost(internetConnectionCheckHost, internetConnectionCheckPort, 5000))
                                            invalidIteration.set(true); //iteration is invalid if monitor cannot reach a known host
                                    } catch (Exception e1) {
                                        log.error("Error checking internet connection");
                                        log.error(e1.getMessage());
                                        invalidIteration.set(true);
                                    }
                                }
                            });
                            threads.add(thread);
                            thread.start();
                        });
                    }
                });

                threads.forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        log.error(e.getMessage());
                    }
                });

                if (invalidIteration.get()) {
                    log.error("Invalid iteration. Skipping");
                    invalidIteration.set(false);
                    return;
                }

                instanceMetricsListBuffer.add(metricsList); //bufferizzare fino alla notifica dell' E prima di attivare l'analisi
                if (getLoopIterationFinished()) {
                    log.debug("Monitor routine completed. Updating Knowledge and notifying the Plan to start the next iteration.\n");
                    knowledgeClient.addMetricsFromBuffer(instanceMetricsListBuffer);
                    instanceMetricsListBuffer.clear();
                    loopIterationFinished.set(false);
                    analyseClient.start();
                }
            } catch (Exception e) {
                knowledgeClient.setFailedModule(Modules.MONITOR);
                log.error(e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error during the monitor execution", e);
            }
        }
    }

    public void changeSchedulingPeriod(int newPeriod) {
        if (monitorRoutine.cancel(false)) {
            log.info("Monitor routine cancelled");
            schedulingPeriod = newPeriod;
            startRoutine();
        } else {
            log.error("Error cancelling monitor routine");
        }
    }

    public void startRoutine() {
        if (monitorRoutine == null || monitorRoutine.isCancelled()) {
            log.info("Monitor routine starting");
            monitorRoutine = taskScheduler.scheduleWithFixedDelay(new MonitorRoutine(), schedulingPeriod);
        } else {
            log.info("Monitor routine already running");
        }
    }

    public void stopRoutine() {
        if (monitorRoutine.cancel(false)) {
            log.info("Monitor routine stopping");
            monitorRoutine = null;
        } else {
            log.error("Error stopping monitor routine");
        }
    }

    public boolean getLoopIterationFinished() {
        return loopIterationFinished.get();
    }

    public void setLoopIterationFinished(boolean loopIterationFinished) {
        knowledgeClient.notifyModuleStart(Modules.MONITOR);
        this.loopIterationFinished.set(loopIterationFinished);
    }

    public void breakpoint(){
        log.info("breakpoint");
    }

    private boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }
}
