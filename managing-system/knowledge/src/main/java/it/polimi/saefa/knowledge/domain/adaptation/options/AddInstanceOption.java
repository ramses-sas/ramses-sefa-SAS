package it.polimi.saefa.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.HashMap;
import java.util.Map;

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


    public AddInstanceOption(String serviceId, String implementationId, Class<? extends AdaptationParamSpecification> goal, String comment) {
        super(serviceId, implementationId, comment);
        super.setAdaptationParametersGoal(goal);
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
        return (isForced() ? "FORCED" : ("Goal: " + getAdaptationParametersGoal().getSimpleName())) + " - Add a new instances of service " + super.getServiceId() + ".\n" + getComment();
    }

}
