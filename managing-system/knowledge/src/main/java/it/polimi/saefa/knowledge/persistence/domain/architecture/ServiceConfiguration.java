package it.polimi.saefa.knowledge.persistence.domain.architecture;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@IdClass(ServiceConfiguration.CompositeKey.class)
public class ServiceConfiguration{

    protected static class CompositeKey implements Serializable {
        private String serviceId;
        @Temporal(TemporalType.TIMESTAMP)
        private Date timestamp;
    }

    @Id
    private String serviceId;

    @Id
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @ElementCollection // <instanceId, weight>
    private Map<String, Integer> loadBalancerWeights = new HashMap<>();
    private String loadBalancerType;

    @ElementCollection // <circuitBreakerName, circuitBreakerConfiguration>
    private Map<String, CircuitBreakerConfiguration> circuitBreakersConfiguration = new HashMap<>();

    @Override
    public String toString() {
        return "Configuration of service: " + serviceId + "\n" +
                "captured at: " + timestamp + "\n" +
                "loadBalancerType: " + loadBalancerType + "\n" +
                //pesi commentati altrimenti la stampa può esplodere. Si possono ottenere nella stampa di una singola istanza
                //(loadBalancerWeights.isEmpty() ? "" : ("loadBalancerWeights: " + loadBalancerWeights + "\n")) +
                (circuitBreakersConfiguration.isEmpty() ? "" : circuitBreakersConfiguration);
    }

    public ServiceConfiguration(String serviceId) {
        this.serviceId = serviceId;
    }

    /*
    @ElementCollection
    private Map<String, String> configuration = new HashMap<>();

    public void addConfigurationItem(String key, String value){
        configuration.put(key, value);
    }
     */

    public void addLoadBalancerWeight(String instance, Integer value){
        loadBalancerWeights.put(instance, value);
    }

    public void addCircuitBreakerProperty(String cbName, String property, String value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!circuitBreakersConfiguration.containsKey(cbName))
            circuitBreakersConfiguration.put(cbName, new CircuitBreakerConfiguration(cbName));
        String setter = "set" + property.substring(0,1).toUpperCase() + property.substring(1);
        circuitBreakersConfiguration.get(cbName).getClass().getDeclaredMethod(setter, String.class).invoke(circuitBreakersConfiguration.get(cbName), value);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceConfiguration that = (ServiceConfiguration) o;
        return serviceId.equals(that.serviceId);
    }

    @Getter
    @Setter
    @Embeddable
    public static class CircuitBreakerConfiguration {
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

        @Override
        public String toString() {
            return "CircuitBreakerConfiguration:\n"+
                "\tcircuitBreakerName: " + circuitBreakerName + "\n" +
                "\tregisterHealthIndicator: " + registerHealthIndicator + "\n" +
                "\tpermittedNumberOfCallsInHalfOpenState: " + permittedNumberOfCallsInHalfOpenState + "\n" +
                "\twaitDurationInOpenState: " + waitDurationInOpenState + "\n" +
                "\tslowCallDurationThreshold: " + slowCallDurationThreshold + "\n" +
                "\tslowCallRateThreshold: " + slowCallRateThreshold + "\n" +
                "\tfailureRateThreshold: " + failureRateThreshold + "\n" +
                "\teventConsumerBufferSize: " + eventConsumerBufferSize + "\n" +
                "\tminimumNumberOfCalls: " + minimumNumberOfCalls + "\n" +
                "\tslidingWindowSize: " + slidingWindowSize + "\n" +
                "\tslidingWindowType: " + slidingWindowType;
        }
        
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
}

