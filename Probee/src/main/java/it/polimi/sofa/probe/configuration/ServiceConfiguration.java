package it.polimi.sofa.probe.configuration;

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
public class ServiceConfiguration {

    protected static class CompositeKey implements Serializable {
        private String serviceId;
        @Temporal(TemporalType.TIMESTAMP)
        private Date timestamp;
    }

    public enum LoadBalancerType{
        WEIGHTED_RANDOM,
        UNKNOWN
    }

    @Id
    private String serviceId;

    @Id
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @ElementCollection // <instanceId, weight>
    private Map<String, Double> loadBalancerWeights;
    private LoadBalancerType loadBalancerType;

    @ElementCollection // <circuitBreakerName, circuitBreakerConfiguration>
    private Map<String, CircuitBreakerConfiguration> circuitBreakersConfiguration = new HashMap<>();

    @Override
    public String toString() {
        return "Configuration of service: " + serviceId + "\n" +
                "\tcaptured at: " + timestamp + "\n\n" +
                "\tloadBalancerType: " + loadBalancerType.name() + "\n" +
                //pesi commentati altrimenti la stampa può esplodere. Si possono ottenere nella stampa di una singola istanza
                //(loadBalancerWeights.isEmpty() ? "" : ("loadBalancerWeights: " + loadBalancerWeights + "\n")) +
                (circuitBreakersConfiguration.isEmpty() ? "" : circuitBreakersConfiguration.values());
    }

    public ServiceConfiguration(String serviceId) {
        this.serviceId = serviceId;
    }



    public void addLoadBalancerWeight(String instanceId, Double value){
        if(loadBalancerWeights == null)
            loadBalancerWeights = new HashMap<>();
        loadBalancerWeights.put(instanceId, value);
    }

    public void addCircuitBreakerProperty(String cbName, String property, String value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!circuitBreakersConfiguration.containsKey(cbName))
            circuitBreakersConfiguration.put(cbName, new CircuitBreakerConfiguration(cbName));
        String setter = "set" + property.substring(0,1).toUpperCase() + property.substring(1);
        circuitBreakersConfiguration.get(cbName).getClass().getDeclaredMethod(setter, String.class).invoke(circuitBreakersConfiguration.get(cbName), value);
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
            return //"CircuitBreakerConfiguration:\n"+
                "\ncircuitBreakerName: " + circuitBreakerName + "\n" +
                "\tregisterHealthIndicator: " + registerHealthIndicator + "\n" +
                "\tpermittedNumberOfCallsInHalfOpenState: " + permittedNumberOfCallsInHalfOpenState + "\n" +
                "\twaitDurationInOpenState: " + waitDurationInOpenState + "\n" +
                "\tslowCallDurationThreshold: " + slowCallDurationThreshold + "\n" +
                "\tslowCallRateThreshold: " + slowCallRateThreshold + "\n" +
                "\tfailureRateThreshold: " + failureRateThreshold + "\n" +
                "\teventConsumerBufferSize: " + eventConsumerBufferSize + "\n" +
                "\tminimumNumberOfCalls: " + minimumNumberOfCalls + "\n" +
                "\tslidingWindowSize: " + slidingWindowSize + "\n" +
                "\tslidingWindowType: " + slidingWindowType + "\n";
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


    /*
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceConfiguration that = (ServiceConfiguration) o;
        return serviceId.equals(that.serviceId);
    }*/
}

