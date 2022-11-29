package it.polimi.sefa.probe.domain.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.sefa.probe.domain.InstanceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class InstanceMetricsSnapshot {

    private Long id;

    private String serviceId; //service name
    private String instanceId; //service implementation id @ip : port

    private InstanceStatus status = InstanceStatus.ACTIVE;
    //@ElementCollection
    // Map<Endpoint, List<HttpRequestMetrics>>
    //public Map<String, List<HttpRequestMetrics>> httpMetrics = new HashMap<>();
    // Map<CircuitBreakerName, CircuitBreakerMetrics>
    private Map<String, CircuitBreakerMetrics> circuitBreakerMetrics = new HashMap<>();
    // Map<HTTP-Method@endpoint, HttpRequestMetrics>
    Map<String, HttpEndpointMetrics> httpMetrics = new HashMap<>();
    private Double cpuUsage;
    private Double diskTotalSpace;
    private Double diskFreeSpace;

    private Date timestamp;

    public InstanceMetricsSnapshot(String serviceId, String instanceId) {
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstanceMetricsSnapshot that = (InstanceMetricsSnapshot) o;

        if (!serviceId.equals(that.serviceId)) return false;
        if (!instanceId.equals(that.instanceId)) return false;
        if (status != that.status) return false;
        if (status == InstanceStatus.UNREACHABLE) return false;
        if (!Objects.equals(circuitBreakerMetrics, that.circuitBreakerMetrics))
            return false;
        return Objects.equals(httpMetrics, that.httpMetrics);
    }

    @JsonIgnore
    public String getServiceImplementationId() {
        return instanceId.split("@")[0];
    }

    public void addHttpMetrics(HttpEndpointMetrics metrics) {
        httpMetrics.put( metrics.getHttpMethod() + "@" + metrics.getEndpoint(), metrics);
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
        if (Double.isNaN(failureRate))
            failureRate = 0;
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.setFailureRate(failureRate);
    }

    public void addCircuitBreakerSlowCallCount(String circuitBreakerName, String outcomeStatus, int count) {
        CircuitBreakerMetrics circuitBreakerMetrics = getOrInitCircuitBreakerMetrics(circuitBreakerName);
        circuitBreakerMetrics.getSlowCallCount().put(CircuitBreakerMetrics.CallOutcomeStatus.valueOf(outcomeStatus.toUpperCase()), count);
    }

    public void addCircuitBreakerSlowCallRate(String circuitBreakerName, double rate) {
        if (Double.isNaN(rate))
            rate = 0;
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

    public boolean isFailed() {
        return status.equals(InstanceStatus.FAILED);
    }

    public boolean isActive() {
        return status.equals(InstanceStatus.ACTIVE);
    }

    public boolean isShutdown() {
        return status.equals(InstanceStatus.SHUTDOWN);
    }

    public boolean isUnreachable() {
        return status.equals(InstanceStatus.UNREACHABLE);
    }

    public boolean isBooting() {
        return status.equals(InstanceStatus.BOOTING);
    }

    @Override
    public String toString() {
        return "Metric id: " + timestamp + " - " + instanceId + " - " + status;
    }


}