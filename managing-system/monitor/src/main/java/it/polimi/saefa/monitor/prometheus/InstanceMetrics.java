package it.polimi.saefa.monitor.prometheus;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InstanceMetrics {
    public String instanceId;
    // Map<Endpoint, List<HttpRequestMetrics>>
    public Map<String, List<HttpRequestMetrics>> httpMetrics = new HashMap<>();
    public Double cpuUsage;
    public Double diskTotalSpace;
    public Double diskFreeSpace;

    public InstanceMetrics(String instanceId) {
        this.instanceId = instanceId;
    }

    public void addHttpMetrics(HttpRequestMetrics metrics) {
        if (!httpMetrics.containsKey(metrics.endpoint)) {
            httpMetrics.put(metrics.endpoint, new LinkedList<>());
        }
        httpMetrics.get(metrics.endpoint).add(metrics);
    }

    public Map<String, List<HttpRequestMetrics>> getHttpMetrics() {
        return this.httpMetrics;
    }

    public List<HttpRequestMetrics> getHttpMetrics(String endpoint) {
        try {
            return httpMetrics.get(endpoint);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public List<HttpRequestMetrics> getHttpMetrics(String endpoint, String method) {
        try {
            return httpMetrics.get(endpoint).stream()
                    .filter(elem -> elem.httpMethod.equals(method))
                    .toList();
        } catch (NullPointerException e) {
            return null;
        }

    }

    public List<HttpRequestMetrics> getHttpMetrics(String endpoint, String method, String outcome) {
        try {
            return httpMetrics.get(endpoint).stream()
                    .filter(elem -> elem.httpMethod.equals(method) && elem.outcome.equals(outcome))
                    .toList();
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "\nMetrics for instance " + instanceId + "{\n" +
                "  httpMetrics = " + httpMetrics + "\n" +
                "  cpuUsage = " + cpuUsage + "\n" +
                "  diskTotalSpace = " + diskTotalSpace + "\n" +
                "  diskFreeSpace = " + diskFreeSpace + "\n" +
                '}';
    }

}