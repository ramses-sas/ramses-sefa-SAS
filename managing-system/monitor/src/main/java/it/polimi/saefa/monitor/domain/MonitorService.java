package it.polimi.saefa.monitor.domain;

import com.netflix.appinfo.InstanceInfo;
import it.polimi.saefa.knowledge.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import it.polimi.saefa.monitor.InstancesSupplier;
import it.polimi.saefa.monitor.externalinterfaces.AnalyseClient;
import it.polimi.saefa.monitor.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.monitor.prometheus.PrometheusParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@EnableScheduling
public class MonitorService {
    private final Set<String> managedServices = new HashSet<>();

    private KnowledgeClient knowledgeClient;
    @Autowired
    private AnalyseClient analyseClient;
    @Autowired
    private InstancesSupplier instancesSupplier;
    @Autowired
    private PrometheusParser prometheusParser;
    @Value("${INTERNET_CONNECTION_CHECK_HOST}")
    private String internetConnectionCheckHost;

    private AtomicBoolean loopIterationFinished = new AtomicBoolean(true);
    private final Queue<List<InstanceMetrics>> instanceMetricsListBuffer = new LinkedList<>(); //linkedlist is FIFO

    public MonitorService(KnowledgeClient knowledgeClient) {
        this.knowledgeClient = knowledgeClient;
        knowledgeClient.getServices().forEach(service -> managedServices.add(service.getServiceId()));
    }

    // ASSUNZIONE: IL PERIODO DEVE ESSERE "ABBASTANZA" MINORE DEL PERIODO DI AGGIORNAMENTO DEL DISCOVERY SERVICE (PER EUREKA DI DEFAULT 90SEC)
    // ASSUNZIONE CHE QUANDO UN'ISTANZA è SU EUREKA HA TERMINATO IL PROCESSO DI STARTUP (ERGO NON C'è INIT DOPO LA REGISTRAZIONE A EUREKA)
    @Scheduled(fixedDelayString = "${SCHEDULING_PERIOD}") //delay in milliseconds
    public void scheduleFixedDelayTask() {
        Map<String, List<InstanceInfo>> services = instancesSupplier.getServicesInstances();
        log.debug("SERVICES: " + services);
        List<InstanceMetrics> metricsList = Collections.synchronizedList(new LinkedList<>());
        List<Thread> threads = new LinkedList<>();
        AtomicBoolean invalidIteration = new AtomicBoolean(false);

        services.forEach((serviceId, serviceInstances) -> {
            if (managedServices.contains(serviceId)) {
                serviceInstances.forEach(instance -> {
                    Thread thread = new Thread(() -> {
                        InstanceMetrics instanceMetrics;
                        try {
                            instanceMetrics = prometheusParser.parse(instance);
                            instanceMetrics.applyTimestamp();
                            log.debug("Adding metric for instance {}", instanceMetrics.getInstanceId());
                            metricsList.add(instanceMetrics);
                        } catch (Exception e) {
                            log.error("Error adding metrics for {}. Considering it as unreachable", instance.getInstanceId());
                            log.error("The exception is: " + e.getMessage());
                            instanceMetrics = new InstanceMetrics(instance.getAppName(), instance.getInstanceId());
                            instanceMetrics.setStatus(InstanceStatus.UNREACHABLE);
                            instanceMetrics.applyTimestamp();
                            metricsList.add(instanceMetrics);
                            try {
                                if (!InetAddress.getByName(internetConnectionCheckHost).isReachable(5000))
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

        if(invalidIteration.get()){
            log.error("Invalid iteration. Skipping");
            return;
        }

        instanceMetricsListBuffer.add(metricsList); //bufferizzare fino alla notifica dell' E prima di attivare l'analisi
        if (getLoopIterationFinished()) {
            for (List<InstanceMetrics> instanceMetricsList : instanceMetricsListBuffer) {
                knowledgeClient.addMetrics(instanceMetricsList);
            }
            instanceMetricsListBuffer.clear();
            setLoopIterationFinished(false);
            analyseClient.start();
        }
    }

    public boolean getLoopIterationFinished() {
        return loopIterationFinished.get();
    }

    public void setLoopIterationFinished(boolean loopIterationFinished) {
        this.loopIterationFinished.set(loopIterationFinished);
    }
}
