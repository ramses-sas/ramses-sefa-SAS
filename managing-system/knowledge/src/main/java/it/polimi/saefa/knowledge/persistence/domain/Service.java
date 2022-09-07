package it.polimi.saefa.knowledge.persistence.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
public class Service {
    private String serviceId;
    private String currentImplementation; //name of the current implementation of the service

    private ServiceConfiguration configuration;
    // <instanceId, Instance>
    private Map<String, Instance> instances = new HashMap<>();
    private Map<String, ServiceImplementation> possibleImplementations = new HashMap<>(); //TODO HOW TO INIT?

    public Service(String serviceId, List<ServiceImplementation> possibleImplementations) {
        this.serviceId = serviceId;
        possibleImplementations.forEach(impl -> {this.possibleImplementations.put(impl.getImplementationId(), impl); impl.setService(this);});

    }

    public void addInstance(Instance instance) {
        instances.put(instance.getInstanceId(), instance);
    }

    public boolean isReachable(){
        for (String instanceAddress : instances.keySet()) {
            if (instances.get(instanceAddress).getCurrentStatus() == InstanceStatus.ACTIVE)
                return true;
        }
        return false;
    }

    public boolean hasInstance(String instanceId){
        return instances.containsKey(instanceId);
    }

    public Instance getOrCreateInstance(String instanceId){
        Instance instance = instances.get(instanceId);
        if (instance == null) {
            instance = new Instance(instanceId, this);
            instances.put(instanceId, instance);
        }
        return instance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return serviceId.equals(service.serviceId);
    }
}
