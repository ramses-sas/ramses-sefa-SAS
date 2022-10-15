package it.polimi.saefa.knowledge.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@NoArgsConstructor
@Getter
public class Service {
    private String serviceId;
    @Setter
    private String currentImplementationId; //name of the current implementation of the service
    private ServiceConfiguration configuration;
    private List<String> dependencies; //names of services that this service depends on
    // <ServiceImplementationId, ServiceImplementation>
    private Map<String, ServiceImplementation> possibleImplementations = new HashMap<>();
    // <AdaptationParameter class, AdaptationParamSpecification>
    private Map<Class<? extends AdaptationParamSpecification>, AdaptationParamSpecification> adaptationParamSpecifications = new HashMap<>();
    @Setter
    private Date latestAdaptationDate = new Date();


    public Service(String serviceId) {
        this.serviceId = serviceId;
    }

    public Service(String serviceId, List<ServiceImplementation> possibleImplementations, List<String> dependencies) {
        this.serviceId = serviceId;
        this.dependencies = dependencies;
        possibleImplementations.forEach(impl -> {this.possibleImplementations.put(impl.getImplementationId(), impl); impl.setServiceId(getServiceId());});
    }

    @JsonIgnore
    public List<Instance> getInstances() {
        return new LinkedList<>(getCurrentImplementation().getInstances().values());
    }

    public void setConfiguration(ServiceConfiguration configuration) {
        this.configuration = configuration;
        if(this.configuration.getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM) {
            if(this.configuration.getLoadBalancerWeights() == null || this.configuration.getLoadBalancerWeights().isEmpty()) {
                getInstances().forEach(instance -> configuration.getLoadBalancerWeights().put(instance.getInstanceId(), 1.0/getInstances().size()));
            }
        }
    }

    @JsonIgnore
    public Map<String, Instance> getInstancesMap() {
        return getCurrentImplementation().getInstances();
    }

    public Instance createInstance(String instanceAddress) {
        return getCurrentImplementation().createInstance(instanceAddress, adaptationParamSpecifications.values().stream().toList());
    }

    // Get the latest "size" VALID values from the valueStack. If "replicateLastValue" is true, the last value is replicated
    // until the size is reached, even if invalid. If "replicateLastValue" is false, the last value is not replicated.
    // The method returns null if the valueStack is empty or if "replicateLastValue" is false and there are less than "size" VALID values.
    public <T extends AdaptationParamSpecification> List<Double> getLatestAnalysisWindowForParam(Class<T> adaptationParamClass, int n) {
        return getCurrentImplementation().getAdaptationParamCollection().getLatestAnalysisWindowForParam(adaptationParamClass, n, false);
    }

    public <T extends AdaptationParamSpecification> List<AdaptationParameter.Value> getValuesHistoryForParam(Class<T> adaptationParamClass) {
        return getCurrentImplementation().getAdaptationParamCollection().getValuesHistoryForParam(adaptationParamClass);
    }

    public <T extends AdaptationParamSpecification> AdaptationParameter.Value getCurrentValueForParam(Class<T> adaptationParamClass) {
        return getCurrentImplementation().getAdaptationParamCollection().getCurrentValueForParam(adaptationParamClass);
    }

    public <T extends AdaptationParamSpecification> void changeCurrentValueForParam(Class<T> adaptationParamClass, double newValue) {
        getCurrentImplementation().getAdaptationParamCollection().changeCurrentValueForParam(adaptationParamClass, newValue);
    }

    public <T extends AdaptationParamSpecification> void invalidateAdaptationParametersHistory(Class<T> adaptationParamClass) {
        getCurrentImplementation().getAdaptationParamCollection().invalidateLatestAndPreviousValuesForParam(adaptationParamClass);
    }

    @JsonIgnore
    public ServiceImplementation getCurrentImplementation() {
        return possibleImplementations.get(currentImplementationId);
    }

    public void setAdaptationParameters(List<AdaptationParamSpecification> specs) {
        for (ServiceImplementation impl : possibleImplementations.values()) {
            impl.setAdaptationParameterSpecifications(specs);
        }
        for (AdaptationParamSpecification spec : specs) {
            adaptationParamSpecifications.put(spec.getClass(), spec);
        }
    }


    public Double getLoadBalancerWeight(Instance instance) {
        return configuration.getLoadBalancerWeights().get(instance.getInstanceId());
    }

    public void setLoadBalancerWeight(Instance instance, double value) {
        if (value == 0.0)
            configuration.getLoadBalancerWeights().remove(instance.getInstanceId());
        else
            configuration.getLoadBalancerWeights().put(instance.getInstanceId(), value);
    }

    public void setLoadBalancerWeights(Map<String, Double> newWeights) {
        configuration.setLoadBalancerWeights(newWeights);
    }


    public void removeInstance(Instance shutdownInstance) {
        getCurrentImplementation().removeInstance(shutdownInstance);
        if(configuration.getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM)
            configuration.getLoadBalancerWeights().remove(shutdownInstance.getInstanceId());
    }

    public void removeShutdownInstances() {
        List<Instance> shutdownInstances = new LinkedList<>();
        getCurrentImplementation().getInstances().values().forEach(instance -> {
            if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN)
                shutdownInstances.add(instance);
        });
        shutdownInstances.forEach(this::removeInstance);
    }

    public Instance getInstance(String instanceId) {
        return getCurrentImplementation().getInstance(instanceId);
    }

    public Map<String, Double> getLoadBalancerWeights() {
        return configuration.getLoadBalancerWeights();
    }

    public boolean shouldConsiderChangingImplementation(){
        return getCurrentImplementation().getImplementationTrust() - getCurrentImplementation().getPenalty() <= 0;
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
                "\tImplemented by: '" + currentImplementationId + "'\n" +
                (configuration == null ? "" : "\t" + configuration.toString().replace("\n", "\n\t").replace(",\t",",\n")) +
                "\n\tPossible Implementations: [" + possibleImplementations + "]\n" +
                "\tDependencies: " + dependencies + "\n" +
                (adaptationParamSpecifications.size() == 0 ? "" : "\tAdaptationParameters: " + adaptationParamSpecifications.values() + "\n" +
                        "\tInstances: " + getInstances().stream().map(Instance::getInstanceId).reduce((s1, s2) -> s1 + ", " + s2).orElse("[]"));
    }
}
