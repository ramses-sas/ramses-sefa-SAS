package it.polimi.saefa.knowledge.persistence.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;
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
    private List<String> dependencies; //names of services that this service depends on

    // <ServiceImplementationId, ServiceImplementation>
    private Map<String, ServiceImplementation> possibleImplementations = new HashMap<>();

    private AdaptationParamSpecification[] adaptationParamSpecifications;



    public Service(String serviceId) {
        this.serviceId = serviceId;
    }

    public Service(String serviceId, List<ServiceImplementation> possibleImplementations, List<String> dependencies) {
        this.serviceId = serviceId;
        this.dependencies = dependencies;
        possibleImplementations.forEach(impl -> {this.possibleImplementations.put(impl.getImplementationId(), impl); impl.setServiceId(getServiceId());});
    }

    public List<Instance> getInstances() {
        return new LinkedList<>(getCurrentImplementationObject().getInstances().values());
    }

    public Instance getOrCreateInstance(String instanceId) {
        return getCurrentImplementationObject().getOrCreateInstance(instanceId, adaptationParamSpecifications);
    }

    @JsonIgnore
    public ServiceImplementation getCurrentImplementationObject() {
        return possibleImplementations.get(currentImplementation);
    }

    @Override
    public String toString() {
        StringBuilder possibleImplementations = new StringBuilder();
        for (String implId : this.possibleImplementations.keySet()) {
            if (!possibleImplementations.isEmpty())
                possibleImplementations.append(", ");
            possibleImplementations.append(implId);
        }

        return "\nService '" + serviceId + "'\n" +
                "\tImplemented by: '" + currentImplementation + "'\n" +
                (configuration == null ? "" : "\t" + configuration.toString().replace("\n", "\n\t").replace(",\t",",\n")) +
                "\n\tPossible Implementations: [" + possibleImplementations + "]\n" +
                "\tDependencies: " + dependencies + "\n" +
                (adaptationParamSpecifications.length == 0 ? "" : "\tAdaptationParameters: " + Arrays.toString(adaptationParamSpecifications) + "\n" +
                "\tInstances: " + getInstances().stream().map(Instance::getInstanceId).reduce((s1, s2) -> s1 + ", " + s2).orElse("[]"));
    }

    public void setAdaptationParameters(AdaptationParamSpecification[] array) {
        if (adaptationParamSpecifications == null) {
            for (ServiceImplementation impl : possibleImplementations.values()) {
                impl.setAdaptationParameterSpecifications(array);
            }
            adaptationParamSpecifications = array;
        }
    }

    //TODO delete and move into implementation
    /*
    public void setAdaptationParameter(Class<? extends AdaptationParamSpecification> clazz, Double value) {
        Optional<AdaptationParamSpecification> adaptationParameter = Arrays.stream(getCurrentImplementationObject().getAdaptationParameterSpecifications()).filter(p -> p.getClass().equals(clazz)).findFirst();
        adaptationParameter.get().addValue(value);
    }

    public <T extends AdaptationParamSpecification> T getAdaptationParameter  (Class<T> clazz) {
        return (T) Arrays.stream(getCurrentImplementationObject().getAdaptationParameterSpecifications()).filter(p -> p.getClass().equals(clazz)).findFirst().orElse(null);
    }

    public AdaptationParamSpecification[] getAdaptationParameters() {
        return getCurrentImplementationObject().getAdaptationParameterSpecifications();
    }


    public void addInstance(Instance instance) {
        instances.put(instance.getInstance(), instance);
    }

    public boolean isReachable() {
        for (String instanceAddress : instances.keySet()) {
            if (instances.get(instanceAddress).getCurrentStatus() == InstanceStatus.ACTIVE)
                return true;
        }
        return false;
    }

    public boolean hasInstance(String instanceId){
        return instances.containsKey(instanceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return serviceId.equals(service.serviceId);
    }

     */

}
