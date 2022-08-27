package it.polimi.saefa.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import prometheus.PrometheusScraper;
import prometheus.types.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;


//@EnableDiscoveryClient
@SpringBootApplication
@RestController
@Slf4j
public class MonitorApplication {
    @Autowired
    PrometheusParser prometheusParser;

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }

    @GetMapping("/")
    public String hello() throws IOException {
        prometheusParser.parse("http://127.0.0.1:58001/actuator/prometheus");
        log.info(String.valueOf(prometheusParser.httpRequestsMetricsRepository.getAllMetrics()));
        return "Hello World!";
    }

}
