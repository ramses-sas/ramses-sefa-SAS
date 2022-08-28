package it.polimi.saefa.monitor;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;


public class CircuitBreakerMetrics {
    public String name;
    public State state;
    public Map<CallOutcomeStatus, Integer> bufferedCallsCount;
    public Map<CallOutcomeStatus, Double> callDuration;
    public Map<CallOutcomeStatus, Double> callMaxDuration;
    public Map<CallOutcomeStatus, Integer> callCount;
    public Map<CallOutcomeStatus, Integer> slowCallCount;
    public int notPermittedCallsCount;
    public double failureRate;
    public double slowCallRate;

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

}
