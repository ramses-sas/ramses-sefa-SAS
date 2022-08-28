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
    // Map<InstanceId, Map<circuitBreakerName, CircuitBreakerMetrics>>
    private final Map<String, Map<String, CircuitBreakerMetrics>> circuitBreakerMetrics;
    // <Key, Value> metrics
    private final Map<String, Map<String, String>> singleValuedMetrics;

    public MetricsRepository() {
        httpMetrics = new HashMap<>();
        singleValuedMetrics = new HashMap<>();
        circuitBreakerMetrics = new HashMap<>();
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

    public void addCircuitBreakerBufferedCalls(String instanceId, String circuitBreakerName, String outcomeStatus, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.bufferedCallsCount.put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
    }

    public void addCircuitBreakerState(String instanceId, String circuitBreakerName, String state, int value){
        if(value == 1){
            CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
            circuitBreakerMetrics.state = CircuitBreakerMetrics.State.valueOf(state.toUpperCase());
        }
    }

    public void addCircuitBreakerCallCountAndDurationSum(String instanceId, String circuitBreakerName, String outcomeStatus, int count, double durationSum) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.callCount.put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
        circuitBreakerMetrics.callDuration.put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), durationSum);
    }

    public void addCircuitBreakerCallMaxDuration(String instanceId, String circuitBreakerName, String outcomeStatus, double duration) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.callMaxDuration.put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), duration);
    }

    public void addCircuitBreakerNotPermittedCallsCount(String instanceId, String circuitBreakerName, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.notPermittedCallsCount = count;
    }

    public void addCircuitBreakerFailureRate(String instanceId, String circuitBreakerName, double failureRate) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.failureRate = failureRate;
    }

    public void addCircuitBreakerSlowCallCount(String instanceId, String circuitBreakerName, String outcomeStatus, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.slowCallCount.put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
    }

    public void addCircuitBreakerSlowCallRate(String instanceId, String circuitBreakerName, double rate) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(instanceId, circuitBreakerName);
        circuitBreakerMetrics.slowCallRate = rate;
    }

    private CircuitBreakerMetrics getOrInitCircuitBreakerMetrics(String instanceId, String circuitBreakerName) {
        if (!circuitBreakerMetrics.containsKey(instanceId)) {
            circuitBreakerMetrics.put(instanceId, new HashMap<>());
        }
        Map<String, CircuitBreakerMetrics> circuitBreakerMetricsMap = circuitBreakerMetrics.get(instanceId);
        if (!circuitBreakerMetricsMap.containsKey(circuitBreakerName)) {
            circuitBreakerMetricsMap.put(circuitBreakerName, new CircuitBreakerMetrics(circuitBreakerName));
        }
        return circuitBreakerMetricsMap.get(circuitBreakerName);
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

    public CircuitBreakerMetrics getCircuitBreakerMetrics(String instanceId, String circuitBreakerName) {
        try {
            return circuitBreakerMetrics.get(instanceId).get(circuitBreakerName);
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
