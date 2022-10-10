package it.polimi.saefa.knowledge.domain.adaptation.options;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("REMOVE_INSTANCES")
public class RemoveInstance extends AdaptationOption {
    private String instanceId;
    @ElementCollection
    // <instanceId, newWeight>
    private Map<String, Double> newWeights;


    public RemoveInstance(String serviceId, String serviceImplementationId, String instanceId, String comment) {
        super(serviceId, serviceImplementationId, comment);
        this.instanceId = instanceId;
    }

    public RemoveInstance(String serviceId, String serviceImplementationId, String instanceId, String comment, boolean isForced) {
        super(serviceId, serviceImplementationId, comment);
        this.instanceId = instanceId;
        super.setForced(isForced);
    }

    @Override
    public String getDescription() {
        return (isForced() ? "FORCED - " : "") + "Remove instances " + instanceId + " of service " + super.getServiceId() + ". " + getComment();
    }
}

