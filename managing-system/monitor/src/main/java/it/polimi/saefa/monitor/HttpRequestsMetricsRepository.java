package it.polimi.saefa.monitor;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Controller
public class HttpRequestsMetricsRepository {
    // Map<Endpoint, List<{method, outcome, status, uri, count, totalDuration}>>
    private Map<String, List<HttpRequestMetrics>> metrics;

    public HttpRequestsMetricsRepository() {
        metrics = new HashMap<>();
    }

    public void addMetrics(HttpRequestMetrics metrics) {
        if (this.metrics.containsKey(metrics.path)) {
            this.metrics.get(metrics.path).add(metrics);
        } else {
            List<HttpRequestMetrics> list = new LinkedList<>();
            list.add(metrics);
            this.metrics.put(metrics.path, list);
        }
    }

    public Map<String, List<HttpRequestMetrics>> getAllMetrics() {
        return this.metrics;
    }

    public List<HttpRequestMetrics> getMetrics(String endpoint) {
        return metrics.get(endpoint);
    }

    public List<HttpRequestMetrics> getMetrics(String endpoint, String method) {
        return metrics.get(endpoint).stream()
                .filter(elem -> elem.httpMethod.equals(method))
                .toList();
    }

    public List<HttpRequestMetrics> getMetrics(String endpoint, String method, String outcome) {
        return metrics.get(endpoint).stream()
                .filter(elem -> elem.httpMethod.equals(method) && elem.outcome.equals(outcome))
                .toList();
    }
}
