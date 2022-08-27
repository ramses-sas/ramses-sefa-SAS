package it.polimi.saefa.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import prometheus.PrometheusScraper;
import prometheus.types.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class PrometheusParser {
    @Autowired
    HttpRequestsMetricsRepository httpRequestsMetricsRepository;

    void parse(String url) {
        List<MetricFamily> list = null;
        try {
            PrometheusScraper scraper = new PrometheusScraper(new URL(url));
            list = scraper.scrape();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        list.forEach(elem -> {
            String propertyName = elem.getName(); //e.g. http_server_requests_seconds
            //MetricType metricType = elem.getType(); //e.g. GAUGE

            elem.getMetrics().forEach(metric -> { //e.g., one metric is the http_server_requests_seconds for the endpoint X
                switch (propertyName) {
                    case "http_server_requests_seconds":
                        handleHttpServerRequestsSeconds((Histogram) metric);
                    default:
                        break;
                }
            });
        } );
    }

    private void handleHttpServerRequestsSeconds(Histogram metric) {
        Map<String, String> labels = metric.getLabels(); //e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        HttpRequestMetrics httpRequestMetrics = new HttpRequestMetrics(
                labels.get("uri"), labels.get("method"), labels.get("outcome"),
                Integer.parseInt(labels.get("status")), metric.getSampleCount(), metric.getSampleSum());
        httpRequestsMetricsRepository.addMetrics(httpRequestMetrics);
    }
}
