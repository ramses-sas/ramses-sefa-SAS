package it.polimi.saefa.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import prometheus.PrometheusScraper;
import prometheus.types.Counter;
import prometheus.types.Gauge;
import prometheus.types.MetricFamily;
import prometheus.types.MetricType;

import java.io.IOException;
import java.net.URL;
import java.util.List;


@EnableDiscoveryClient
@SpringBootApplication
@RestController
@Slf4j
public class MonitorApplication {

    public static void main(String[] args) {

        SpringApplication.run(MonitorApplication.class, args);


        List<MetricFamily> list = null;
        try {
            PrometheusScraper scraper = new PrometheusScraper(new URL("http://127.0.0.1:58081/actuator/prometheus"));
            list = scraper.scrape();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        list.forEach(elem -> {
            log.warn("AAAAA");
            log.warn(elem.getName());
            elem.getMetrics().forEach(elem2 -> {
                log.warn(String.valueOf(elem2.getLabels()));
                if (elem.getType() == MetricType.COUNTER) {
                    Counter counterElem = (Counter) elem2;
                    log.warn(String.valueOf(counterElem.getValue()));
                }
                if (elem.getType() == MetricType.GAUGE) {
                    Gauge gaugeElem = (Gauge) elem2;
                    log.warn(String.valueOf(gaugeElem.getValue()));
                }
            });
            log.warn("BBBBB");
        } );
    }

    @GetMapping("/")
    public String hello() throws IOException {

        return "Hello World!";
    }

}
