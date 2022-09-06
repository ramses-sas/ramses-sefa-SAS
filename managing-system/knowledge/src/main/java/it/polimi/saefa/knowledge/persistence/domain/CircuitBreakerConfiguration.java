package it.polimi.saefa.knowledge.persistence.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;

@Getter
@Setter
@Embeddable
public class CircuitBreakerConfiguration {
    private String circuitBreakerName;

    private Boolean registerHealthIndicator;
    private Integer permittedNumberOfCallsInHalfOpenState;
    private Integer waitDurationInOpenState;
    private Integer slowCallDurationThreshold;
    private Integer slowCallRateThreshold;
    private Integer failureRateThreshold;
    private Integer eventConsumerBufferSize;
    private Integer minimumNumberOfCalls;
    private Integer slidingWindowSize;
    private String  slidingWindowType;

    public CircuitBreakerConfiguration(String circuitBreakerName) {
        this.circuitBreakerName = circuitBreakerName;
    }

    public CircuitBreakerConfiguration() { }

    public void setRegisterHealthIndicator(String registerHealthIndicator) {
        this.registerHealthIndicator = Boolean.valueOf(registerHealthIndicator);
    }

    public void setPermittedNumberOfCallsInHalfOpenState(String permittedNumberOfCallsInHalfOpenState) {
        this.permittedNumberOfCallsInHalfOpenState = Integer.valueOf(permittedNumberOfCallsInHalfOpenState);
    }

    public void setWaitDurationInOpenState(String waitDurationInOpenState) {
        this.waitDurationInOpenState = Integer.valueOf(waitDurationInOpenState);
    }

    public void setSlowCallDurationThreshold(String slowCallDurationThreshold) {
        this.slowCallDurationThreshold = Integer.valueOf(slowCallDurationThreshold);
    }

    public void setSlowCallRateThreshold(String slowCallRateThreshold) {
        this.slowCallRateThreshold = Integer.valueOf(slowCallRateThreshold);
    }

    public void setFailureRateThreshold(String failureRateThreshold) {
        this.failureRateThreshold = Integer.valueOf(failureRateThreshold);
    }

    public void setEventConsumerBufferSize(String eventConsumerBufferSize) {
        this.eventConsumerBufferSize = Integer.valueOf(eventConsumerBufferSize);
    }

    public void setMinimumNumberOfCalls(String minimumNumberOfCalls) {
        this.minimumNumberOfCalls = Integer.valueOf(minimumNumberOfCalls);
    }

    public void setSlidingWindowSize(String slidingWindowSize) {
        this.slidingWindowSize = Integer.valueOf(slidingWindowSize);
    }

}
