package it.polimi.saefa.knowledge.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.util.*;

@Entity
@Data
@NoArgsConstructor
public class InstanceMetrics {
    @Id
    @GeneratedValue
    private Long id;
    private String serviceId;
    private String instanceId;
    //@ElementCollection
    // Map<Endpoint, List<HttpRequestMetrics>>
    //public Map<String, List<HttpRequestMetrics>> httpMetrics = new HashMap<>();
    @OneToMany
    // Map<CircuitBreakerName, CircuitBreakerMetrics>
    private Map<String, CircuitBreakerMetrics> circuitBreakerMetrics = new HashMap<>();
    @OneToMany
    List<HttpRequestMetrics> httpMetrics = new LinkedList<>();
    private Double cpuUsage;
    private Double diskTotalSpace;
    private Double diskFreeSpace;
    private Date timestamp;

    public InstanceMetrics(String serviceId, String instanceId) {
        this.instanceId = instanceId;
        this.serviceId = serviceId;
    }


    /*public void addHttpMetrics(HttpRequestMetrics metrics) {
        if (!httpMetrics.containsKey(metrics.endpoint)) {
            httpMetrics.put(metrics.endpoint, new LinkedList<>());
        }
        httpMetrics.get(metrics.endpoint).add(metrics);
    }*/



    public void addHttpMetrics(HttpRequestMetrics metrics) {
        httpMetrics.add(metrics);
    }
    public void applyTimestamp() {
        timestamp = new Date();
    }

    public void addCircuitBreakerBufferedCalls(String circuitBreakerName, String outcomeStatus, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.getBufferedCallsCount().put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
    }

    public void addCircuitBreakerState(String circuitBreakerName, String state, int value){
        if(value == 1){
            CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
            circuitBreakerMetrics.setState(CircuitBreakerMetrics.State.valueOf(state.toUpperCase()));
        }
    }

    public void addCircuitBreakerCallCountAndDurationSum(String circuitBreakerName, String outcomeStatus, int count, double durationSum) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.getCallCount().put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
        circuitBreakerMetrics.getCallDuration().put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), durationSum);
    }

    public void addCircuitBreakerCallMaxDuration(String circuitBreakerName, String outcomeStatus, double duration) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.getCallMaxDuration().put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), duration);
    }

    public void addCircuitBreakerNotPermittedCallsCount(String circuitBreakerName, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.setNotPermittedCallsCount(count);
    }

    public void addCircuitBreakerFailureRate(String circuitBreakerName, double failureRate) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.setFailureRate(failureRate);
    }

    public void addCircuitBreakerSlowCallCount(String circuitBreakerName, String outcomeStatus, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.getSlowCallCount().put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
    }

    public void addCircuitBreakerSlowCallRate(String circuitBreakerName, double rate) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.setSlowCallRate(rate);
    }

    private CircuitBreakerMetrics getOrInitCircuitBreakerMetrics(String circuitBreakerName) {
        if (!circuitBreakerMetrics.containsKey(circuitBreakerName)) {
            circuitBreakerMetrics.put(circuitBreakerName, new CircuitBreakerMetrics(circuitBreakerName));
        }
        return circuitBreakerMetrics.get(circuitBreakerName);
    }

    public CircuitBreakerMetrics getCircuitBreakerMetrics(String circuitBreakerName) {
        try {
            return circuitBreakerMetrics.get(circuitBreakerName);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /*
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
    }*/

    @Override
    public String toString() {
        return "\nMetrics for instance " + instanceId + " {\n" +
                "  date = " + timestamp + "\n" +
                "  httpMetrics = " + httpMetrics + "\n" +
                "  cpuUsage = " + cpuUsage + "\n" +
                "  diskTotalSpace = " + diskTotalSpace + "\n" +
                "  diskFreeSpace = " + diskFreeSpace + "\n" +
                "  circuitBreakerMetrics = " + circuitBreakerMetrics +
                "\n}";
    }


}