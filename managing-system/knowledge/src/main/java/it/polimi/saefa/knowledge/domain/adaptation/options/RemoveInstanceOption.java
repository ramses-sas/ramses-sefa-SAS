package it.polimi.saefa.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("REMOVE_INSTANCES")
public class RemoveInstanceOption extends AdaptationOption {
    private String instanceId;
    @ElementCollection
    // <instanceId, newWeight>
    private Map<String, Double> newWeights;


    public RemoveInstanceOption(String serviceId, String serviceImplementationId, String instanceId, String comment) {
        super(serviceId, serviceImplementationId, comment);
        this.instanceId = instanceId;
    }

    public RemoveInstanceOption(String serviceId, String serviceImplementationId, String instanceId, String comment, boolean isForced) {
        super(serviceId, serviceImplementationId, comment);
        this.instanceId = instanceId;
        super.setForced(isForced);
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        return (isForced() ? "FORCED" : ("Goal: " + getAdaptationParametersGoal().getSimpleName())) + " - Remove instances " + instanceId + " of service " + super.getServiceId() + ".\n" + getComment();
    }
}

