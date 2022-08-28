package it.polimi.saefa.monitor;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import prometheus.PrometheusScraper;
import prometheus.types.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class PrometheusParser {

    public MetricsRepository parse(InstanceInfo instanceInfo) {
        MetricsRepository metricsRepository = new MetricsRepository();
        String url = instanceInfo.getHomePageUrl()+"actuator/prometheus";
        List<MetricFamily> metricFamilies;
        try {
            PrometheusScraper scraper = new PrometheusScraper(new URL(url));
            metricFamilies = scraper.scrape();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        metricFamilies.forEach(metricFamily -> {
            String propertyName = metricFamily.getName(); //e.g. http_server_requests_seconds
            //MetricType metricType = elem.getType(); //e.g. GAUGE

            metricFamily.getMetrics().forEach(metric -> { //e.g., one metric is the http_server_requests_seconds for the endpoint X
                Map<String, String> labels = metric.getLabels();
                switch (propertyName) {
                    case "http_server_requests_seconds":
                        metricsRepository.addHttpMetrics(instanceInfo.getInstanceId(), handleHttpServerRequestsSeconds((Histogram) metric));
                        break;
                    case "resilience4j_circuitbreaker_buffered_calls":
                        metricsRepository.addCircuitBreakerBufferedCalls(instanceInfo.getInstanceId(), labels.get("name"),
                                labels.get("kind"), (int) ((Gauge) metric).getValue());
                        break;
                    case "resilience4j_circuitbreaker_state":
                        metricsRepository.addCircuitBreakerState(instanceInfo.getInstanceId(), labels.get("name"),
                                labels.get("state"), (int) ((Gauge) metric).getValue());
                        break;
                    case "resilience4j_circuitbreaker_calls_seconds":
                        metricsRepository.addCircuitBreakerCallCountAndDurationSum(instanceInfo.getInstanceId(), labels.get("name"),
                                labels.get("kind"), (int) ((Summary) metric).getSampleCount(), ((Summary) metric).getSampleSum());
                        break;
                    case "resilience4j_circuitbreaker_calls_seconds_max":
                        metricsRepository.addCircuitBreakerCallMaxDuration(instanceInfo.getInstanceId(), labels.get("name"),
                                labels.get("kind"), ((Gauge)metric).getValue());
                    case "resilience4j_circuitbreaker_not_permitted_calls_total":
                        metricsRepository.addCircuitBreakerNotPermittedCallsCount(instanceInfo.getInstanceId(), labels.get("name"),
                                (int) ((Gauge) metric).getValue());
                        break;

                    case "resilience4j_circuitbreaker_slow_call_rate":
                        metricsRepository.addCircuitBreakerSlowCallRate(instanceInfo.getInstanceId(), labels.get("name"),
                                ((Gauge) metric).getValue());
                        break;
                    case "resilience4j_circuitbreaker_slow_calls":
                        metricsRepository.addCircuitBreakerSlowCallCount(instanceInfo.getInstanceId(), labels.get("name"),
                                labels.get("kind"), (int) ((Gauge) metric).getValue());
                        break;
                    case "resilience4j_circuitbreaker_failure_rate":
                        metricsRepository.addCircuitBreakerFailureRate(instanceInfo.getInstanceId(), labels.get("name"),
                                ((Gauge) metric).getValue());
                        break;
                    default:
                        break;
                }
            });
        } );
        return metricsRepository;
    }

    private HttpRequestMetrics handleHttpServerRequestsSeconds(Histogram metric) {
        Map<String, String> labels = metric.getLabels(); //e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        return new HttpRequestMetrics(
                labels.get("uri"), labels.get("method"), labels.get("outcome"),
                Integer.parseInt(labels.get("status")), metric.getSampleCount(), metric.getSampleSum());
    }


}
