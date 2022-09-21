package it.polimi.saefa.knowledge.persistence.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.values.AdaptationParamCollection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ServiceImplementation {
    private String serviceId; //implemented service name
    private String implementationId; //specific implementation name

    // <instanceId, Instance>
    private Map<String, Instance> instances = new HashMap<>();
    private AdaptationParamCollection adaptationParamCollection = new AdaptationParamCollection();


    private double costPerInstance;
    private double costPerRequest; // tipo scatto alla risposta
    private double costPerSecond; //cost per second a richiesta (equivale a una sorta di costo per processing time)
    private double costPerBoot; //costo per avvio di un'istanza
    private double score; //valutazione di quanto è preferibile questa implementazione rispetto ad altre
    private double penalty = 0; //penalità associata a quanto adattamento è stato fatto su questa implementazione

    public ServiceImplementation(String implementationId, double costPerInstance, double costPerRequest, double costPerSecond, double costPerBoot, double score) {
        this.implementationId = implementationId;
        this.costPerInstance = costPerInstance;
        this.costPerRequest = costPerRequest;
        this.costPerSecond = costPerSecond;
        this.costPerBoot = costPerBoot;
        this.score = score;
    }

    public boolean addInstance(Instance instance) {
        if(instance.getServiceImplementationId().equals(implementationId)) {
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

    public Instance getOrCreateInstance(String instanceId) {
        Instance instance = instances.get(instanceId);
        if (instance == null) {
            if(instanceId.split("@")[0].equals(implementationId)) {
                instance = new Instance(instanceId, serviceId);
                instances.put(instanceId, instance);
            }
        }
        return instance;
    }

    public double addPenalty(double penalty) {
        this.penalty += penalty;
        return this.penalty;
    }

    protected void setAdaptationParameterSpecifications(AdaptationParamSpecification[] array) {
        for(AdaptationParamSpecification specification : array) {
            adaptationParamCollection.createHistory(specification);
        }
    }

}
