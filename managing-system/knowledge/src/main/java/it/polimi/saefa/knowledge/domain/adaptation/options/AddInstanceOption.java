package it.polimi.saefa.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DiscriminatorValue("ADD_INSTANCE")
public class AddInstanceOption extends AdaptationOption {
    @ElementCollection
    // <instanceId, newWeight>
    private Map<String, Double> oldInstancesNewWeights;
    private Double newInstanceWeight;
    @ElementCollection
    private List<String> instancesToShutdownIds = new LinkedList<>(); //There could be instances whose weight have gone below the shutdown threshold after redistributing the weights


    public AddInstanceOption(String serviceId, String implementationId, Class<? extends QoSSpecification> goal, String comment) {
        super(serviceId, implementationId, comment);
        super.setQosGoal(goal);
    }

    public AddInstanceOption(String serviceId, String implementationId, String comment, boolean isForced) {
        super(serviceId, implementationId, comment);
        super.setForced(isForced);
    }

    public Map<String, Double> getFinalWeights(String newInstanceId) {
        if(oldInstancesNewWeights == null)
            return null;
        Map<String, Double> finalWeights = new HashMap<>(oldInstancesNewWeights);
        finalWeights.put(newInstanceId, newInstanceWeight);
        return finalWeights;
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        return (isForced() ? "FORCED" : ("Goal: " + getQosGoal().getSimpleName())) + " - Add a new instance. Service:" + super.getServiceId() + (!instancesToShutdownIds.isEmpty() ? (".\n\t\t\t\t\tInstances to remove: " + instancesToShutdownIds + "\n\t\t\t\t\t") : " ")  + getComment();
    }

}
