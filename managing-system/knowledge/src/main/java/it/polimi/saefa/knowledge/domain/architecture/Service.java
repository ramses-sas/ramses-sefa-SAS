package it.polimi.saefa.knowledge.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSHistory;
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
    // <QoS class, QoSSpecification>
    private Map<Class<? extends QoSSpecification>, QoSSpecification> qoSSpecifications = new HashMap<>();
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
    public boolean isInTransitionState() {
        return getInstances().stream().anyMatch(instance -> instance.getCurrentStatus() == InstanceStatus.BOOTING || instance.getCurrentStatus() == InstanceStatus.SHUTDOWN);
    }

    @JsonIgnore
    public List<Instance> getInstances() {
        return new LinkedList<>(getCurrentImplementation().getInstances().values());
    }

    @JsonIgnore
    public List<Instance> getBootingInstances(){
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.BOOTING).toList());
    }

    @JsonIgnore
    public List<Instance> getShutdownInstances(){
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.SHUTDOWN).toList());
    }

    @JsonIgnore
    public List<Instance> getFailedInstances(){
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.FAILED).toList());
    }

    @JsonIgnore
    public List<Instance> getActiveInstances(){
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.ACTIVE).toList());
    }

    @JsonIgnore
    public List<Instance> getUnreachableInstances(){
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.UNREACHABLE).toList());
    }

    @JsonIgnore
    public List<Instance> getAvailableInstances() {
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.ACTIVE || instance.getCurrentStatus() == InstanceStatus.UNREACHABLE).toList());
    }

    @JsonIgnore
    public List<Instance> getAvailableAndBootingInstances() {
        return new LinkedList<>(getCurrentImplementation().getInstances().values().stream().filter(instance -> instance.getCurrentStatus() == InstanceStatus.ACTIVE || instance.getCurrentStatus() == InstanceStatus.UNREACHABLE || instance.getCurrentStatus() == InstanceStatus.BOOTING).toList());
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
        return getCurrentImplementation().createInstance(instanceAddress, qoSSpecifications.values().stream().toList());
    }

    // Get the latest "size" VALID values from the valueStack. If "replicateLastValue" is true, the last value is replicated
    // until the size is reached, even if invalid. If "replicateLastValue" is false, the last value is not replicated.
    // The method returns null if the valueStack is empty or if "replicateLastValue" is false and there are less than "size" VALID values.
    public <T extends QoSSpecification> List<Double> getLatestAnalysisWindowForQoS(Class<T> qosClass, int n) {
        return getCurrentImplementation().getQoSCollection().getLatestAnalysisWindowForQoS(qosClass, n, false);
    }

    public <T extends QoSSpecification> List<QoSHistory.Value> getValuesHistoryForQoS(Class<T> qosClass) {
        return getCurrentImplementation().getQoSCollection().getValuesHistoryForQoS(qosClass);
    }

    public <T extends QoSSpecification> QoSHistory.Value getCurrentValueForQoS(Class<T> qosClass) {
        return getCurrentImplementation().getQoSCollection().getCurrentValueForQoS(qosClass);
    }

    public <T extends QoSSpecification> QoSHistory.Value changeCurrentValueForQoS(Class<T> qosClass, double newValue, Date date) {
        return getCurrentImplementation().getQoSCollection().changeCurrentValueForQoS(qosClass, newValue, date);
    }

    public <T extends QoSSpecification> void invalidateQoSHistory(Class<T> qosClass) {
        getCurrentImplementation().getQoSCollection().invalidateLatestAndPreviousValuesForQoS(qosClass);
    }

    @JsonIgnore
    public ServiceImplementation getCurrentImplementation() {
        return possibleImplementations.get(currentImplementationId);
    }

    public void setAllQoS(List<QoSSpecification> specs) {
        for (ServiceImplementation impl : possibleImplementations.values()) {
            impl.setAllQoSSpecifications(specs);
        }
        for (QoSSpecification spec : specs) {
            qoSSpecifications.put(spec.getClass(), spec);
        }
    }


    public Double getLoadBalancerWeight(Instance instance) {
        return configuration.getLoadBalancerWeights().get(instance.getInstanceId());
    }

    public void setLoadBalancerWeights(Map<String, Double> newWeights) {
        configuration.setLoadBalancerWeights(newWeights);
    }


    public void removeInstance(Instance shutdownInstance) {
        getCurrentImplementation().removeInstance(shutdownInstance);
        /*if(configuration.getLoadBalancerType() == ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM)
            if(configuration.getLoadBalancerWeights().remove(shutdownInstance.getInstanceId())!=null)
                throw new RuntimeException("THIS SHOULD NOT HAPPEN: Error while removing instance from load balancer weights");
         */
    }

    /*
    public void removeShutdownInstances() {
        List<Instance> shutdownInstances = new LinkedList<>();
        getCurrentImplementation().getInstances().values().forEach(instance -> {
            if (instance.getCurrentStatus() == InstanceStatus.SHUTDOWN)
                shutdownInstances.add(instance);
        });
        shutdownInstances.forEach(this::removeInstance);
    }

     */

    public Instance getInstance(String instanceId) {
        return getCurrentImplementation().getInstance(instanceId);
    }

    public Map<String, Double> getLoadBalancerWeights() {
        return configuration.getLoadBalancerWeights();
    }

    public boolean shouldConsiderChangingImplementation(){
        return possibleImplementations.size() > 1 && (getCurrentImplementation().getTrust() - getCurrentImplementation().getPenalty() <= 0);
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
                (qoSSpecifications.size() == 0 ? "" : "\tQoS: " + qoSSpecifications.values() + "\n" +
                        "\tInstances: " + getInstances().stream().map(Instance::getInstanceId).reduce((s1, s2) -> s1 + ", " + s2).orElse("[]"));
    }
}
