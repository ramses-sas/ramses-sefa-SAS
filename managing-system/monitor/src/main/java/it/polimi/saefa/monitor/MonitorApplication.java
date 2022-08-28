package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@EnableDiscoveryClient
@SpringBootApplication
@RestController
@Slf4j
@EnableScheduling
public class MonitorApplication {

    @Autowired
    PrometheusParser prometheusParser;

    @Autowired
    InstancesDiscoveryService instancesDiscoveryService;


    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }

    @Scheduled(fixedDelay = 10_000) //delay in milliseconds
    public void scheduleFixedDelayTask() {
        Map<String, List<InstanceInfo>> services = instancesDiscoveryService.getServicesInstances();
        //log.debug("Services: {}", services);
        services.forEach((serviceName, serviceInstances) -> {
            log.debug("Getting data for service {}", serviceName);
            serviceInstances.forEach(prometheusParser::parse);
            serviceInstances.forEach(instance -> {
                MetricsRepository metricsRepository = prometheusParser.parse(instance);
                Map<String, List<HttpRequestMetrics>> instanceMetrics = metricsRepository.getHttpMetrics(instance.getInstanceId());
                if (instanceMetrics != null) {
                    log.debug(instanceMetrics.toString());
                }
            });
        });
    }

}
