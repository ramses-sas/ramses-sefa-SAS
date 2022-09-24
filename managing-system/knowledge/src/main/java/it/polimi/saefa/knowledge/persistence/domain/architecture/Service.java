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

    private Map<Class<? extends AdaptationParamSpecification>, AdaptationParamSpecification> adaptationParamSpecifications = new HashMap<>();
    //private AdaptationParamSpecification[] adaptationParamSpecifications;



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
        return getCurrentImplementationObject().getOrCreateInstance(instanceId, adaptationParamSpecifications.values().stream().toList());
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
                (adaptationParamSpecifications.size() == 0 ? "" : "\tAdaptationParameters: " + adaptationParamSpecifications.values() + "\n" +
                "\tInstances: " + getInstances().stream().map(Instance::getInstanceId).reduce((s1, s2) -> s1 + ", " + s2).orElse("[]"));
    }

    public void setAdaptationParameters(List<AdaptationParamSpecification> specs) {
        for (ServiceImplementation impl : possibleImplementations.values()) {
            impl.setAdaptationParameterSpecifications(specs);
        }
        for (AdaptationParamSpecification spec : specs) {
            adaptationParamSpecifications.put(spec.getClass(), spec);
        }
    }
}
