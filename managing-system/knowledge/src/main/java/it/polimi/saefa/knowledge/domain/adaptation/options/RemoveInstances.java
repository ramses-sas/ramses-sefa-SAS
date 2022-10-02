package it.polimi.saefa.knowledge.domain.adaptation.options;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DiscriminatorValue("REMOVE_INSTANCES")
public class RemoveInstances extends AdaptationOption {
    @ElementCollection
    private List<String> instanceIdList;

    public RemoveInstances(String serviceId, String implementationId, String instanceIdList) {
        super(serviceId, implementationId);
        this.instanceIdList = List.of(instanceIdList);
    }

    public RemoveInstances(String serviceId, String serviceImplementationId, List<String> instanceIdList) {
        super(serviceId, serviceImplementationId);
        this.instanceIdList = instanceIdList;
    }

    @Override
    public String getDescription() {
        return "Remove instances " + instanceIdList + " of service " + super.getServiceId();
    }
}

