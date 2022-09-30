package it.polimi.saefa.knowledge.domain.adaptation.options;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("REMOVE_INSTANCE")
public class RemoveInstance extends AdaptationOption {
    private String instanceId;

    public RemoveInstance(String serviceId, String implementationId, String instanceId) {
        super(serviceId, implementationId);
        this.instanceId = instanceId;
    }

    @Override
    public String getDescription() {
        return "Remove instance " + instanceId + " of service " + super.getServiceId();
    }
}

