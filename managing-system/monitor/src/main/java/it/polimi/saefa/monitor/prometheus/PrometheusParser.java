package it.polimi.saefa.monitor.prometheus;

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
    public InstanceMetrics parse(InstanceInfo instanceInfo) {
        InstanceMetrics instanceMetrics = new InstanceMetrics(instanceInfo.getInstanceId());
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
                log.debug("Metric {}: {}", metric.getName(), metric.getLabels());
                Map<String, String> labels = metric.getLabels();
                switch (propertyName) {
                    case PrometheusMetrics.HTTP_REQUESTS_TIME ->
                            instanceMetrics.addHttpMetrics(handleHttpServerRequestsSeconds((Histogram) metric));
                    case PrometheusMetrics.DISK_FREE_SPACE ->
                            instanceMetrics.diskFreeSpace = ((Gauge) metric).getValue();
                    case PrometheusMetrics.DISK_TOTAL_SPACE ->
                            instanceMetrics.diskTotalSpace = ((Gauge) metric).getValue();
                    case PrometheusMetrics.CPU_USAGE -> instanceMetrics.cpuUsage = ((Gauge) metric).getValue();
                    case PrometheusMetrics.CB_BUFFERED_CALLS -> instanceMetrics.addCircuitBreakerBufferedCalls(labels.get("name"),
                                    labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_STATE -> instanceMetrics.addCircuitBreakerState(labels.get("name"),
                            labels.get("state"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_CALLS_SECONDS -> instanceMetrics.addCircuitBreakerCallCountAndDurationSum(labels.get("name"),
                            labels.get("kind"), (int) ((Summary) metric).getSampleCount(), ((Summary) metric).getSampleSum());
                    case PrometheusMetrics.CB_CALLS_SECONDS_MAX ->  instanceMetrics.addCircuitBreakerCallMaxDuration(labels.get("name"),
                            labels.get("kind"), ((Gauge)metric).getValue());
                    case PrometheusMetrics.CB_NOT_PERMITTED_CALLS_TOTAL -> instanceMetrics.addCircuitBreakerNotPermittedCallsCount(labels.get("name"),
                            (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALL_RATE -> instanceMetrics.addCircuitBreakerSlowCallRate(labels.get("name"),
                            ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALLS -> instanceMetrics.addCircuitBreakerSlowCallCount(labels.get("name"),
                            labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_FAILURE_RATE -> instanceMetrics.addCircuitBreakerFailureRate(labels.get("name"),
                            ((Gauge) metric).getValue());
                    default -> {
                    }
                }
            });
        } );
        return instanceMetrics;
    }

    private HttpRequestMetrics handleHttpServerRequestsSeconds(Histogram metric) {
        Map<String, String> labels = metric.getLabels(); //e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        return new HttpRequestMetrics(
                labels.get("uri"), labels.get("method"), labels.get("outcome"),
                Integer.parseInt(labels.get("status")), metric.getSampleCount(), metric.getSampleSum());
    }


}
