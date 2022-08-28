package it.polimi.saefa.monitor;

import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


//@Controller
public class MetricsRepository {
    // Map<InstanceId, Map<path, List<{method, outcome, status, uri, count, totalDuration}>>>
    private final Map<String, Map<String, List<HttpRequestMetrics>>> httpMetrics;
    // <Key, Value> metrics
    private final Map<String, Map<String, String>> singleValuedMetrics;

    public MetricsRepository() {
        httpMetrics = new HashMap<>();
        singleValuedMetrics = new HashMap<>();
    }

    public void addHttpMetrics(String instanceId, HttpRequestMetrics metrics) {
        if (!httpMetrics.containsKey(instanceId)) {
            httpMetrics.put(instanceId, new HashMap<>());
        }
        Map<String, List<HttpRequestMetrics>> endpointMetrics = httpMetrics.get(instanceId);
        if (!endpointMetrics.containsKey(metrics.path)) {
            endpointMetrics.put(metrics.path, new LinkedList<>());
        }
        endpointMetrics.get(metrics.path).add(metrics);
    }

    public Map<String, Map<String, List<HttpRequestMetrics>>> getHttpMetrics() {
        return this.httpMetrics;
    }

    public Map<String, List<HttpRequestMetrics>> getHttpMetrics(String instanceId) {
        return this.httpMetrics.get(instanceId);
    }

    public List<HttpRequestMetrics> getHttpMetrics(String instanceId, String endpoint) {
        try {
            return httpMetrics.get(instanceId).get(endpoint);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public List<HttpRequestMetrics> getHttpMetrics(String instanceId, String endpoint, String method) {
        try {
            return httpMetrics.get(instanceId).get(endpoint).stream()
                    .filter(elem -> elem.httpMethod.equals(method))
                    .toList();
        } catch (NullPointerException e) {
            return null;
        }

    }

    public List<HttpRequestMetrics> getHttpMetrics(String instanceId, String endpoint, String method, String outcome) {
        try {
            return httpMetrics.get(instanceId).get(endpoint).stream()
                    .filter(elem -> elem.httpMethod.equals(method) && elem.outcome.equals(outcome))
                    .toList();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void addSingleValuedMetrics(String instanceId, String name, String value) {
        if (!singleValuedMetrics.containsKey(instanceId)) {
            singleValuedMetrics.put(instanceId, new HashMap<>());
        }
        singleValuedMetrics.get(instanceId).put(name, value);
    }

    public Map<String, Map<String, String>> getSingleValuedMetrics() {
        return this.singleValuedMetrics;
    }

    public String getSingleValuedMetric(String instanceId, String name) {
        try {
            return singleValuedMetrics.get(instanceId).get(name);
        } catch (NullPointerException e) {
            return null;
        }
    }
}
