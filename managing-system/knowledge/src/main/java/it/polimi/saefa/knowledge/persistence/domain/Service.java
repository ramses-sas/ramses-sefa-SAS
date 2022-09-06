package it.polimi.saefa.knowledge.persistence.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@NoArgsConstructor
@Getter
@Setter
public class Service {
    private String name;
    private String currentImplementation; //name of the current implementation of the service

    private ServiceConfiguration configuration;
    private Map<String, Instance> instances = new HashMap<>();
    private Set<String> possibleImplementations = new HashSet<>(); //TODO HOW TO INIT?

    public Service(String name, String currentImplementation) {
        this.name = name;
        this.currentImplementation = currentImplementation;
        possibleImplementations.add(currentImplementation);
    }

    public Service(String name, String currentImplementation, String firstInstanceAddress) {
        this.name = name;
        this.currentImplementation = currentImplementation;
        instances.put(firstInstanceAddress, new Instance(firstInstanceAddress, this));
        possibleImplementations.add(currentImplementation);
    }

    public boolean isReachable(){
        for(String instanceAddress : instances.keySet()){
            if(instances.get(instanceAddress).getCurrentStatus() == InstanceStatus.ACTIVE){
                return true;
            }
        }
        return false;
    }

    public boolean hasInstance(String instanceId){
        return instances.containsKey(instanceId);
    }

    public Instance getOrCreateInstance(String instanceId){
        Instance instance = instances.get(instanceId);
        if(instance == null){
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

        return name.equals(service.name);
    }
}
