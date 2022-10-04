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


    public RemoveInstances(String serviceId, String serviceImplementationId, List<String> instanceIdList, String comment) {
        super(serviceId, serviceImplementationId, comment);
        this.instanceIdList = instanceIdList;
    }

    public RemoveInstances(String serviceId, String serviceImplementationId, List<String> instanceIdList, String comment, boolean force) {
        super(serviceId, serviceImplementationId, comment);
        this.instanceIdList = instanceIdList;
        this.setForced(force);
    }



    @Override
    public String getDescription() {
        return "Remove instances " + instanceIdList + " of service " + super.getServiceId() + ". " + getComment();
    }
}

