package it.polimi.saefa.monitor.prometheus;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
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
    public InstanceMetrics parse(InstanceInfo instanceInfo) {
        InstanceMetrics instanceMetrics = new InstanceMetrics(instanceInfo.getInstanceId());
        String url = instanceInfo.getHomePageUrl()+"actuator/prometheus";
        List<MetricFamily> list;
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
                log.debug("Metric {}: {}", metric.getName(), metric.getLabels());
                switch (propertyName) {
                    case PrometheusMetrics.HTTP_REQUESTS_TIME ->
                            instanceMetrics.addHttpMetrics(handleHttpServerRequestsSeconds((Histogram) metric));
                    case PrometheusMetrics.DISK_FREE_SPACE ->
                            instanceMetrics.diskFreeSpace = ((Gauge) metric).getValue();
                    case PrometheusMetrics.DISK_TOTAL_SPACE ->
                            instanceMetrics.diskTotalSpace = ((Gauge) metric).getValue();
                    case PrometheusMetrics.CPU_USAGE ->
                            instanceMetrics.cpuUsage = ((Gauge) metric).getValue();
                    default -> {}
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
