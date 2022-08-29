package it.polimi.saefa.knowledge.persistence;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CircuitBreakerMetrics {
    private String name;
    private State state;
    private Map<CallOutcomeStatus, Integer> bufferedCallsCount;
    private Map<CallOutcomeStatus, Double> callDuration;
    private Map<CallOutcomeStatus, Double> callMaxDuration;
    private Map<CallOutcomeStatus, Integer> callCount;
    private Map<CallOutcomeStatus, Integer> slowCallCount;
    private int notPermittedCallsCount;
    private double failureRate;
    private double slowCallRate;

    public CircuitBreakerMetrics(String name) {
        this.name = name;
        this.bufferedCallsCount = new HashMap<>();
        this.callDuration = new HashMap<>();
        this.callMaxDuration = new HashMap<>();
        this.callCount = new HashMap<>();
        this.slowCallCount = new HashMap<>();
    }
    public enum State {
        CLOSED, OPEN, HALF_OPEN, DISABLED
    }
    public enum CallOutcomeStatus {
        SUCCESSFUL, FAILED, IGNORED, NOT_PERMITTED
    }

    public double getAverageDuration(CallOutcomeStatus status) {
        return callDuration.get(status)/callCount.get(status);
    }

    public int getTotalCallsCount() {
        return callCount.values().stream().mapToInt(Integer::intValue).sum() + notPermittedCallsCount;
    }

    @Override
    public String toString() {
        return "CircuitBreakerMetrics { " +
                "\n\t\tname='" + name + "'" +
                ",\n\t\tstate=" + state +
                ",\n\t\tbufferedCallsCount=" + bufferedCallsCount +
                ",\n\t\tcallDuration=" + callDuration +
                ",\n\t\tcallMaxDuration=" + callMaxDuration +
                ",\n\t\tcallCount=" + callCount +
                ",\n\t\tslowCallCount=" + slowCallCount +
                ",\n\t\tnotPermittedCallsCount=" + notPermittedCallsCount +
                ",\n\t\tfailureRate=" + failureRate +
                ",\n\t\tslowCallRate=" + slowCallRate +
                ",\n\t\ttotalCallCount=" + getTotalCallsCount() +
                "}\n";
    }
}
