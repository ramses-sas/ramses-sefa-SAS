package it.polimi.saefa.knowledge.persistence.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
public class ServiceConfiguration {
    @Id
    private String serviceId;
    @ElementCollection
    private Map<String, String> configuration = new HashMap<>();
    @ElementCollection
    private Map<String, Double> loadBalancerWeight = new HashMap<>();
    private String loadBalancerType;
    @ElementCollection
    private Map<String, String> circuitBreaker = new HashMap<>();

    public ServiceConfiguration(String serviceId) {
        this.serviceId = serviceId;
    }

    public void addConfigurationItem(String key, String value){
        configuration.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceConfiguration that = (ServiceConfiguration) o;
        return serviceId.equals(that.serviceId);
    }
}

