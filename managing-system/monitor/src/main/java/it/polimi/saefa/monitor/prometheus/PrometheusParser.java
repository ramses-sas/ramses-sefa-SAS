package it.polimi.saefa.monitor.prometheus;

import com.netflix.appinfo.InstanceInfo;
import it.polimi.saefa.knowledge.domain.metrics.HttpEndpointMetrics;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${ACTUATOR_RELATIVE_PATH}")
    private String actuatorRelativePath;

    public InstanceMetrics parse(InstanceInfo instanceInfo) {
        InstanceMetrics instanceMetrics = new InstanceMetrics(instanceInfo.getAppName(), instanceInfo.getInstanceId());
        List<MetricFamily> metricFamilies;
        try {
            URL url = new URL(instanceInfo.getHomePageUrl());
            url = new URL(url, actuatorRelativePath+"/prometheus");
            PrometheusScraper scraper = new PrometheusScraper(url);
            metricFamilies = scraper.scrape();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, HttpEndpointMetrics> httpMetricsMap = new HashMap<>();

        metricFamilies.forEach(metricFamily -> {
            String propertyName = metricFamily.getName(); //e.g. http_server_requests_seconds
            //MetricType metricType = elem.getType(); //e.g. GAUGE
            metricFamily.getMetrics().forEach(metric -> { //e.g., one metric is the http_server_requests_seconds for the endpoint X
                //log.debug("Metric {}: {}", metric.getName(), metric.getLabels());
                Map<String, String> labels = metric.getLabels();
                switch (propertyName) {
                    case PrometheusMetrics.HTTP_REQUESTS_TIME ->
                            handleHttpServerRequestsSeconds(httpMetricsMap, (Histogram) metric);
                    case PrometheusMetrics.HTTP_REQUESTS_MAX_TIME ->
                            handleHttpServerRequestsMaxDuration(httpMetricsMap, (Gauge) metric);
                    case PrometheusMetrics.DISK_FREE_SPACE ->
                            instanceMetrics.setDiskFreeSpace(((Gauge) metric).getValue());
                    case PrometheusMetrics.DISK_TOTAL_SPACE ->
                            instanceMetrics.setDiskTotalSpace(((Gauge) metric).getValue());
                    case PrometheusMetrics.CPU_USAGE -> instanceMetrics.setCpuUsage(((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_BUFFERED_CALLS -> instanceMetrics.addCircuitBreakerBufferedCalls(labels.get("name"),
                                    labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_STATE -> instanceMetrics.addCircuitBreakerState(labels.get("name"),
                            labels.get("state"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_CALLS_SECONDS -> instanceMetrics.addCircuitBreakerCallCountAndDurationSum(labels.get("name"),
                            labels.get("kind"), (int) ((Summary) metric).getSampleCount(), ((Summary) metric).getSampleSum());
                    case PrometheusMetrics.CB_CALLS_SECONDS_MAX ->  instanceMetrics.addCircuitBreakerCallMaxDuration(labels.get("name"),
                            labels.get("kind"), ((Gauge)metric).getValue());
                    case PrometheusMetrics.CB_NOT_PERMITTED_CALLS_TOTAL -> instanceMetrics.addCircuitBreakerNotPermittedCallsCount(labels.get("name"),
                            (int) ((Counter) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALL_RATE -> instanceMetrics.addCircuitBreakerSlowCallRate(labels.get("name"),
                            ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALLS -> instanceMetrics.addCircuitBreakerSlowCallCount(labels.get("name"),
                            labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_FAILURE_RATE -> instanceMetrics.addCircuitBreakerFailureRate(labels.get("name"),
                            ((Gauge) metric).getValue());
                    default -> { }
                }
            });
        } );
        instanceMetrics.setHttpMetrics(httpMetricsMap);
        return instanceMetrics;
    }

    private boolean isAnExcludedUrl(String url) {
        return url.contains("/actuator/");
    }

    private void handleHttpServerRequestsSeconds(Map<String, HttpEndpointMetrics> httpMetricsMap, Histogram metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsDetails(labels.get("outcome"), Integer.parseInt(labels.get("status")), (int) metric.getSampleCount(), metric.getSampleSum());
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
    }

    private void handleHttpServerRequestsMaxDuration(Map<String, HttpEndpointMetrics> httpMetricsMap, Gauge metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsMaxDuration(labels.get("outcome"), metric.getValue());
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
    }


}
