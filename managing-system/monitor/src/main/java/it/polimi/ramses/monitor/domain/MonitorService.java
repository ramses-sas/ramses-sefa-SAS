package it.polimi.ramses.monitor.domain;

import it.polimi.ramses.knowledge.domain.Modules;
import it.polimi.ramses.monitor.externalinterfaces.KnowledgeClient;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.domain.metrics.InstanceMetricsSnapshot;
import it.polimi.ramses.monitor.externalinterfaces.ProbeClient;
import it.polimi.ramses.monitor.externalinterfaces.AnalyseClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@org.springframework.stereotype.Service
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
    private ProbeClient probeClient;

    @Getter
    @Value("${SCHEDULING_PERIOD}")
    private int schedulingPeriod = 5000; // monitor scheduling period [ms]

    private final AtomicBoolean loopIterationFinished = new AtomicBoolean(true);
    private final Queue<List<InstanceMetricsSnapshot>> instanceMetricsListBuffer = new LinkedList<>(); //linkedlist is FIFO

    // TODO remove after testing
    private Map<String, Service> servicesMap;

    public MonitorService(KnowledgeClient knowledgeClient, ThreadPoolTaskScheduler taskScheduler) {
        this.knowledgeClient = knowledgeClient;
        this.taskScheduler = taskScheduler;
        servicesMap = knowledgeClient.getServicesMap();
        servicesMap.values().forEach(service -> managedServices.add(service.getServiceId()));
    }

    /*
    // decomment to start routine on startup
    @PostConstruct
    public void initRoutine() {
        monitorRoutine = taskScheduler.scheduleWithFixedDelay(new MonitorRoutine(), schedulingPeriod);
    }
     */

    // ASSUNZIONE: IL PERIODO DEVE ESSERE "ABBASTANZA" MINORE DEL PERIODO DI AGGIORNAMENTO DEL DISCOVERY SERVICE (PER EUREKA DI DEFAULT 90SEC)
    // ASSUNZIONE CHE QUANDO UN'ISTANZA è SU EUREKA HA TERMINATO IL PROCESSO DI STARTUP (ERGO NON C'è INIT DOPO LA REGISTRAZIONE A EUREKA)
    class MonitorRoutine implements Runnable {
        @Override
        public void run() {
            log.debug("\nA new Monitor routine iteration started");
            try {
                /*Map<String, ServiceInfo> probeServiceRuntimeArchitecture = probeClient.getSystemArchitecture();
                servicesMap = knowledgeClient.getServicesMap();
                probeServiceRuntimeArchitecture.forEach((serviceId, serviceInfo) -> {
                    // TODO remove after testing
                    if (servicesMap.containsKey(serviceId)) {
                        if (servicesMap.get(serviceId).getInstances().size() != serviceInfo.getInstances().size()) {
                            log.warn("!!! Probe and Knowledge Service are not in sync for service {}", serviceId);
                            log.warn("!!! Knowledge: {} - [{}]", serviceId, servicesMap.get(serviceId).getInstances().stream().map(i -> i.getInstanceId() + "_" + i.getCurrentStatus()).reduce((a, b) -> a + ", " + b).orElse("no instances"));
                        }
                        log.debug("Service: {} - [{}]", serviceId, serviceInfo.getInstances().stream().reduce((a, b) -> a + ", " + b).orElse("no instances"));
                    }
                });*/

                List<InstanceMetricsSnapshot> metricsList = Collections.synchronizedList(new LinkedList<>());
                List<Thread> threads = new LinkedList<>();
                AtomicBoolean invalidIteration = new AtomicBoolean(false);

                managedServices.forEach(serviceId -> {
                    Thread thread = new Thread( () -> {
                        try {
                            //if (probeServiceRuntimeArchitecture.containsKey(serviceId)) {
                            List<InstanceMetricsSnapshot> instancesSnapshots = probeClient.takeSnapshot(serviceId);
                            if (instancesSnapshots == null) {
                                invalidIteration.set(true);
                            } else {
                                metricsList.addAll(instancesSnapshots);
                            }
                            //} else {
                             //   log.warn("No instances for service {} is not in the runtime architecture!", serviceId);
                            //}
                        } catch (Exception e) {
                            log.error("Error while taking snapshot for service {}", serviceId, e);
                            invalidIteration.set(true);
                        }
                    });
                    threads.add(thread);
                    thread.start();
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
                    log.debug("Monitor routine completed. Updating Knowledge and notifying the Analyse to start the next iteration.\n");
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
