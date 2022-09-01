package it.polimi.saefa.knowledge.persistence.components;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ServiceConfiguration {
    private final String serviceId;
    private Map<String, String> configuration = new HashMap<>();
    private Map<String, Double> loadBalancerWeight = new HashMap<>();
    private String loadBalancerType;
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

