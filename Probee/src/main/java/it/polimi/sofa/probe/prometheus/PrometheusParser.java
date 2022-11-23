package it.polimi.sofa.probe.prometheus;

import com.netflix.appinfo.InstanceInfo;
import it.polimi.sofa.probe.domain.metrics.HttpEndpointMetrics;
import it.polimi.sofa.probe.domain.metrics.InstanceMetricsSnapshot;
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

    public InstanceMetricsSnapshot parse(InstanceInfo instanceInfo) {
        InstanceMetricsSnapshot instanceMetricsSnapshot = new InstanceMetricsSnapshot(instanceInfo.getAppName(), instanceInfo.getInstanceId());
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
                            handleHttpServerRequestsTotalDurationMs(httpMetricsMap, (Histogram) metric);
                    case PrometheusMetrics.HTTP_REQUESTS_MAX_TIME ->
                            handleHttpServerRequestsMaxDuration(httpMetricsMap, (Gauge) metric);
                    case PrometheusMetrics.DISK_FREE_SPACE ->
                            instanceMetricsSnapshot.setDiskFreeSpace(((Gauge) metric).getValue());
                    case PrometheusMetrics.DISK_TOTAL_SPACE ->
                            instanceMetricsSnapshot.setDiskTotalSpace(((Gauge) metric).getValue());
                    case PrometheusMetrics.CPU_USAGE ->
                            instanceMetricsSnapshot.setCpuUsage(((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_BUFFERED_CALLS ->
                            instanceMetricsSnapshot.addCircuitBreakerBufferedCalls(labels.get("name"), labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_STATE ->
                            instanceMetricsSnapshot.addCircuitBreakerState(labels.get("name"), labels.get("state"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_CALLS_SECONDS ->
                            instanceMetricsSnapshot.addCircuitBreakerCallCountAndDurationSum(labels.get("name"), labels.get("kind"), (int) ((Summary) metric).getSampleCount(), ((Summary) metric).getSampleSum());
                    case PrometheusMetrics.CB_CALLS_SECONDS_MAX ->
                            instanceMetricsSnapshot.addCircuitBreakerCallMaxDuration(labels.get("name"), labels.get("kind"), ((Gauge)metric).getValue());
                    case PrometheusMetrics.CB_NOT_PERMITTED_CALLS_TOTAL ->
                            instanceMetricsSnapshot.addCircuitBreakerNotPermittedCallsCount(labels.get("name"), (int) ((Counter) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALL_RATE ->
                            instanceMetricsSnapshot.addCircuitBreakerSlowCallRate(labels.get("name"), ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_SLOW_CALLS ->
                            instanceMetricsSnapshot.addCircuitBreakerSlowCallCount(labels.get("name"), labels.get("kind"), (int) ((Gauge) metric).getValue());
                    case PrometheusMetrics.CB_FAILURE_RATE ->
                            instanceMetricsSnapshot.addCircuitBreakerFailureRate(labels.get("name"), ((Gauge) metric).getValue());
                    default -> { }
                }
            });
        } );
        instanceMetricsSnapshot.setHttpMetrics(httpMetricsMap);
        return instanceMetricsSnapshot;
    }

    private boolean isAnExcludedUrl(String url) {
        return url.contains("/actuator/");
    }

    private void handleHttpServerRequestsTotalDurationMs(Map<String, HttpEndpointMetrics> httpMetricsMap, Histogram metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsDetails(labels.get("outcome"), Integer.parseInt(labels.get("status")), (int) metric.getSampleCount(), metric.getSampleSum()*1000);
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
    }

    private void handleHttpServerRequestsMaxDuration(Map<String, HttpEndpointMetrics> httpMetricsMap, Gauge metric) {
        Map<String, String> labels = metric.getLabels();//e.g. labels' key for http_server_requests_seconds are [exception, method, uri, status]
        if (isAnExcludedUrl(labels.get("uri")))
            return;
        HttpEndpointMetrics metrics = httpMetricsMap.getOrDefault(labels.get("method") + "@" + labels.get("uri"), new HttpEndpointMetrics(labels.get("uri"), labels.get("method")));
        metrics.addOrSetOutcomeMetricsMaxDuration(labels.get("outcome"), metric.getValue()*1000);
        httpMetricsMap.putIfAbsent(labels.get("method") + "@" + labels.get("uri"), metrics);
    }


}
