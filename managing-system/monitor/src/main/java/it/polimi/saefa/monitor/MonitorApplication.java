package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import it.polimi.saefa.monitor.externalinterfaces.KnowledgeClient;
import it.polimi.saefa.monitor.prometheus.PrometheusParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
@RestController
@Slf4j
@EnableScheduling
public class MonitorApplication {
    @Autowired
    private KnowledgeClient knowledgeClient;
    @Autowired
    PrometheusParser prometheusParser;
    @Autowired
    private EurekaClient discoveryClient;

    private final Queue<List<InstanceMetrics>> instanceMetricsListBuffer = new LinkedList<>(); //linkedlist is FIFO
    private boolean canStartLoop = true;

    public Map<String, List<InstanceInfo>> getServicesInstances() {
        List<Application> applications = discoveryClient.getApplications().getRegisteredApplications();
        Map<String, List<InstanceInfo>> servicesInstances = new HashMap<>();
        applications.forEach(application -> {
            if (application.getName().endsWith("-SERVICE")) {
                List<InstanceInfo> applicationsInstances = application.getInstances();
                servicesInstances.put(application.getName(), applicationsInstances);
            }
        });
        return servicesInstances;
    }

    @Scheduled(fixedDelay = 100_000) //delay in milliseconds
    public void scheduleFixedDelayTask() {
        Map<String, List<InstanceInfo>> services = getServicesInstances();
        List<InstanceMetrics> metricsList = new LinkedList<>();

        services.forEach((serviceName, serviceInstances) -> {
            log.debug("Getting data for service {}", serviceName);
            serviceInstances.forEach(instance -> {
                InstanceMetrics instanceMetrics;
                try {
                    instanceMetrics = prometheusParser.parse(instance);
                    instanceMetrics.applyTimestamp();
                    log.debug(instanceMetrics.toString());
                    metricsList.add(instanceMetrics);
                } catch (Exception e) {
                    log.error("Error adding metrics for {}. Considering it as down", instance.getInstanceId());
                    log.error(e.getMessage());
                }
            });
        });
        instanceMetricsListBuffer.add(metricsList); //bufferizzare fino alla notifica dell' E prima di attivare l'analisi
        if(getCanStartLoop()){
            for (List<InstanceMetrics> instanceMetricsList : instanceMetricsListBuffer) {
                knowledgeClient.addMetrics(instanceMetricsList);
            }
            instanceMetricsListBuffer.clear();
            //Notificare la nuova configurazione se presente.
            setCanStartLoop(false);
            //notifica l'analysis
        }
    }

    public synchronized boolean getCanStartLoop(){
        return canStartLoop;
    }

    public synchronized void setCanStartLoop(boolean canStartLoop){
        this.canStartLoop = canStartLoop;
    }

    /*
    //TODO Se non si presenta il caso in cui la macchina è raggiungibile ma lo status non è UP, integrare questa logica nell'altro scheduler
    @Scheduled(fixedDelay = 50_000) //delay in milliseconds
    public void periodicCheckDownStatus() {
        Map<String, List<InstanceInfo>> services = getServicesInstances();
        services.forEach((serviceName, serviceInstances) -> {
            serviceInstances.forEach(instance -> {

                    String url = instance.getHomePageUrl() + "actuator/health";
                    try {
                        ResponseEntity<Object> response = new RestTemplate().getForEntity(url, Object.class);
                        String status = response.getBody().toString();
                        if (!status.contains("status=UP"))
                            throw new RuntimeException("Instance is down");
                        else
                            log.debug("Instance {} of service {} is up", instance.getHostName() + ":" + instance.getPort(), instance.getAppName());
                    } catch (Exception e) {
                        log.warn("Instance {} is down", url);
                        if(getServicesInstances().get(serviceName).contains(instance)) {
                            //this check is done to avoid the case in which the instance is gracefully
                            // disconnected from eureka since the original services list is not updated.
                            // Still, it can happen between the check and the actual call to the service, so how to solve?
                            InstanceMetrics instanceMetrics = new InstanceMetrics(instance.getAppName(), instance.getInstanceId());
                            instanceMetrics.setStatus(InstanceStatus.FAILED);
                            instanceMetrics.applyTimestamp();
                            knowledgeClient.addMetrics(instanceMetrics);

                        }
                    }

            });
        });
    }

    */

    @GetMapping("/startLoop")
    public void start() {
        setCanStartLoop(true);
    }

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
