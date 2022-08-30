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
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    @Scheduled(fixedDelay = 10_000) //delay in milliseconds
    public void scheduleFixedDelayTask() {
        Map<String, List<InstanceInfo>> services = getServicesInstances();
        //log.debug("Services: {}", services);
        services.forEach((serviceName, serviceInstances) -> {
            log.debug("Getting data for service {}", serviceName);
            serviceInstances.forEach(instance -> {
                try {
                    InstanceMetrics instanceMetrics = prometheusParser.parse(instance);
                    instanceMetrics.applyTimestamp();
                    log.debug(instanceMetrics.toString());
                    knowledgeClient.addMetrics(instanceMetrics);
                } catch (Exception e) {
                    log.error("Error adding metrics for {}", instance.getInstanceId());
                    log.error(e.getMessage());
                }
            });
        });
    }


    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }

}
