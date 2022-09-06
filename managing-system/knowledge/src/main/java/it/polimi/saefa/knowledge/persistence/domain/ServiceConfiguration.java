package it.polimi.saefa.knowledge.persistence.domain;

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
    @ElementCollection
    private Map<String, String> configuration = new HashMap<>();
    @ElementCollection
    private Map<String, Integer> loadBalancerWeight = new HashMap<>(); // <instanceId, weight>
    private String loadBalancerType;
    @ElementCollection
    private Map<String, CircuitBreakerConfiguration> circuitBreakerConfigurations = new HashMap<>();

    public ServiceConfiguration(String serviceId) {
        this.serviceId = serviceId;
    }

    public void addConfigurationItem(String key, String value){
        configuration.put(key, value);
    }

    public void addLoadBalancerWeight(String instance, Integer value){
        loadBalancerWeight.put(instance, value);
    }

    public void addCircuitBreakerProperty(String cbName, String property, String value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!circuitBreakerConfigurations.containsKey(cbName))
            circuitBreakerConfigurations.put(cbName, new CircuitBreakerConfiguration(cbName));
        String setter = "set" + property.substring(0,1).toUpperCase() + property.substring(1);
        circuitBreakerConfigurations.get(cbName).getClass().getDeclaredMethod(setter, String.class).invoke(circuitBreakerConfigurations.get(cbName), value);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceConfiguration that = (ServiceConfiguration) o;
        return serviceId.equals(that.serviceId);
    }
}

