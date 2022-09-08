package it.polimi.saefa.knowledge.persistence.domain.architecture;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.AdaptationParameter;
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
    // <instanceId, ServiceImplementation>
    private Map<String, ServiceImplementation> possibleImplementations = new HashMap<>();

    private AdaptationParameter[] adaptationParameters = {};

    public Service(String serviceId) {
        this.serviceId = serviceId;
    }

    public Service(String serviceId, List<ServiceImplementation> possibleImplementations) {
        this.serviceId = serviceId;
        possibleImplementations.forEach(impl -> {this.possibleImplementations.put(impl.getImplementationId(), impl); impl.setServiceId(getServiceId());});
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

    public Instance getOrCreateInstance(String instanceId) {
        Instance instance = instances.get(instanceId);
        if (instance == null) {
            instance = new Instance(instanceId, serviceId);
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

    @Override
    public String toString() {
        StringBuilder possibleImplementations = new StringBuilder();
        for (String implId : this.possibleImplementations.keySet()) {
            if (!possibleImplementations.isEmpty())
                possibleImplementations.append(", ");
            possibleImplementations.append(implId);
        }

        return "\nService '" + serviceId + "'" + "\n" +
                "\tImplemented by: '" + currentImplementation + "'" + "\n" +
                (configuration == null ? "" : "\t" + configuration.toString().replace("\n", "\n\t").replace(",\t",",\n")) +
                "\tInstances: " + instances.keySet() + "\n" +
                "\tPossible Implementations: [" + possibleImplementations + "]\n" +
                (adaptationParameters.length == 0 ? "" : "\tadaptationParameters: " + Arrays.toString(adaptationParameters));
    }
}
