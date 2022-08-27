package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import prometheus.PrometheusScraper;
import prometheus.types.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.stream.Collectors;


//@EnableDiscoveryClient
@SpringBootApplication
@RestController
@Slf4j
@EnableScheduling
public class MonitorApplication {
    @Autowired
    PrometheusParser prometheusParser;

    @Autowired
    private EurekaClient discoveryClient;
    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }

    @Scheduled(fixedDelay = 100_000) //delay in milliseconds
    public void scheduleFixedDelayTask() {
        log.warn("\n" + printInstances());
        getPrometheusProperties();
    }

    public void getPrometheusProperties() {
        Map<String, List<URL>> services = getServicesInstances();
        Set<String> servicesNames = services.keySet();
        servicesNames.forEach(serviceName -> {
            List<URL> instances = services.get(serviceName);
            log.warn("Service: " + serviceName);
            instances.forEach(instance -> {
                try {
                    PrometheusScraper scraper = new PrometheusScraper(new URL(instance + "actuator/prometheus"));
                    List<MetricFamily> metricFamilies = scraper.scrape();
                    metricFamilies.forEach(metricFamily -> {
                        log.warn(metricFamily.getName());
                        metricFamily.getMetrics().forEach(elem2 -> {

                            log.warn("LABELS: " + elem2.getLabels());
                            if (metricFamily.getType() == MetricType.COUNTER) {
                                Counter counterElem = (Counter) elem2;
                                log.warn("COUNTER: " + counterElem.getValue());
                            }
                            if (metricFamily.getType() == MetricType.GAUGE) {
                                Gauge gaugeElem = (Gauge) elem2;
                                log.warn("GAUGE: " + gaugeElem.getValue());
                            }
                        });
                        log.warn("----------------------------------------------------");
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            log.warn("******************************************************");
        });
    }

    public Map<String, List<URL>> getServicesInstances(){
        List<Application> applications = discoveryClient.getApplications().getRegisteredApplications();
        Map<String, List<URL>> servicesInstances = new HashMap<>();
        applications.forEach(application -> {
            List<InstanceInfo> applicationsInstances = application.getInstances();
           //servicesInstances.put(application.getName(), applicationsInstances.stream().map(InstanceInfo::getHomePageUrl).collect(Collectors.toList()));
            servicesInstances.put(application.getName(), applicationsInstances.stream().map(InstanceInfo::getHomePageUrl).toList().stream().map(elem -> {
                try {
                    return new URL(elem);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
        });
        return servicesInstances;
    }

    public String printInstances(){
        StringBuilder toReturn = new StringBuilder();

        Map<String, List<URL>> servicesInstances = getServicesInstances();
        servicesInstances.forEach((serviceName, serviceInstances) -> {
            toReturn.append(serviceName).append(": ");
            serviceInstances.forEach(serviceInstance -> {
                toReturn.append(serviceInstance).append(" ");
            });
            toReturn.append("\n");
        } );

        return toReturn.toString();
    }
    
    @GetMapping("/")
    public String hello() throws IOException {
        prometheusParser.parse("http://127.0.0.1:58001/actuator/prometheus");
        log.info(String.valueOf(prometheusParser.httpRequestsMetricsRepository.getAllMetrics()));
        return printInstances();
    }

}
