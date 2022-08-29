package it.polimi.saefa.knowledge.persistence;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@NoArgsConstructor
public class CircuitBreakerMetrics {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private State state;
    @ElementCollection
    private Map<CallOutcomeStatus, Integer> bufferedCallsCount;
    @ElementCollection
    private Map<CallOutcomeStatus, Double> callDuration;
    @ElementCollection
    private Map<CallOutcomeStatus, Double> callMaxDuration;
    @ElementCollection
    private Map<CallOutcomeStatus, Integer> callCount;
    @ElementCollection
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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Map<CallOutcomeStatus, Integer> getBufferedCallsCount() {
        return bufferedCallsCount;
    }

    public void setBufferedCallsCount(Map<CallOutcomeStatus, Integer> bufferedCallsCount) {
        this.bufferedCallsCount = bufferedCallsCount;
    }

    public Map<CallOutcomeStatus, Double> getCallDuration() {
        return callDuration;
    }

    public void setCallDuration(Map<CallOutcomeStatus, Double> callDuration) {
        this.callDuration = callDuration;
    }

    public Map<CallOutcomeStatus, Double> getCallMaxDuration() {
        return callMaxDuration;
    }

    public void setCallMaxDuration(Map<CallOutcomeStatus, Double> callMaxDuration) {
        this.callMaxDuration = callMaxDuration;
    }

    public Map<CallOutcomeStatus, Integer> getCallCount() {
        return callCount;
    }

    public void setCallCount(Map<CallOutcomeStatus, Integer> callCount) {
        this.callCount = callCount;
    }

    public Map<CallOutcomeStatus, Integer> getSlowCallCount() {
        return slowCallCount;
    }

    public void setSlowCallCount(Map<CallOutcomeStatus, Integer> slowCallCount) {
        this.slowCallCount = slowCallCount;
    }

    public int getNotPermittedCallsCount() {
        return notPermittedCallsCount;
    }

    public void setNotPermittedCallsCount(int notPermittedCallsCount) {
        this.notPermittedCallsCount = notPermittedCallsCount;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }

    public double getSlowCallRate() {
        return slowCallRate;
    }

    public void setSlowCallRate(double slowCallRate) {
        this.slowCallRate = slowCallRate;
    }

}
