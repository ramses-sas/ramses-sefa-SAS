package it.polimi.saefa.knowledge.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ServiceImplementation {
    private String serviceId; //implemented service name
    private String implementationId; //specific implementation id

    // <instanceId, Instance>
    private Map<String, Instance> instances = new HashMap<>();
    private QoSCollection qoSCollection = new QoSCollection();
    // <qosClass, qosBenchmark>
    private final Map<Class<? extends QoSSpecification>, Double> qoSBenchmarks = new HashMap<>();

    private double score; //valutazione di quanto è preferibile questa implementazione rispetto ad altre
    private int implementationTrust; //valutazione di quanto è affidabile questa implementazione
    private int penalty = 0; //penalità associata a quanto adattamento è stato fatto su questa implementazione
    //private double riskFactor; //fattore di rischio associato a quanto è rischioso avviare un'intanza di questa implementazione senza conoscenze pregresse sui QoS
    // ratio between the number of requests processed by an instance and the number of requests processed in an ideal case (when the load if equally split) that triggers the shutdown of an instance
    // should be seen as rate/threshold (i.e., shutdown the instances which process less than this threshold percentage with respect to the ideal case)
    private double instanceLoadShutdownThreshold;

    public ServiceImplementation(String implementationId, double score, int implementationTrust, double instanceLoadShutdownThreshold) {
        this.implementationId = implementationId;
        this.score = score;
        this.implementationTrust = implementationTrust;
        this.instanceLoadShutdownThreshold = instanceLoadShutdownThreshold;
    }

    public double getBenchmark(Class<? extends QoSSpecification> qosSpecificationClass) {
        return qoSBenchmarks.get(qosSpecificationClass);
    }

    public void setBenchmark(Class<? extends QoSSpecification> qosSpecificationClass, Double benchmark) {
        qoSBenchmarks.put(qosSpecificationClass, benchmark);
    }

    public boolean addInstance(Instance instance) {
        if (instance.getServiceImplementationId().equals(implementationId)) {
            instances.put(instance.getInstanceId(), instance);
            return true;
        }
        return false;
    }

    @JsonIgnore
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

    public Instance createInstance(String instanceAddress, List<QoSSpecification> qoSSpecifications) {
        String instanceId = implementationId + "@" + instanceAddress;
        if(instances.containsKey(instanceId))
            throw new RuntimeException("Instance already exists");
        Instance instance = new Instance(instanceId, serviceId);
        for (QoSSpecification specification : qoSSpecifications) {
            instance.getQoSCollection().createHistory(specification);
            instance.getQoSCollection().changeCurrentValueForQoS(specification.getClass(), getBenchmark(specification.getClass()));
        }
        instances.put(instanceId, instance);
        return instance;
    }

    public double addPenalty(double penalty) {
        this.penalty += penalty;
        return this.penalty;
    }

    protected void setAllQoSSpecifications(List<QoSSpecification> specs) {
        for (QoSSpecification specification : specs) {
            qoSCollection.createHistory(specification);
        }
    }

    public void removeInstance(Instance shutdownInstance) {
        instances.remove(shutdownInstance.getInstanceId());
    }

    public Instance getInstance(String instanceId) {
        return instances.get(instanceId);
    }


    public void incrementPenalty() {
        this.penalty += 1;
    }
}
