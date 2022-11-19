package it.polimi.ramses.knowledge.domain.adaptation.options;

import it.polimi.ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DiscriminatorValue("CHANGE_IMPLEMENTATION")
public class ChangeImplementationOption extends AdaptationOption {
    @ElementCollection
    private List<String> possibleImplementations;
    private String newImplementationId;
    private int numberOfInstances;

    public ChangeImplementationOption(String serviceId, String serviceImplementationId, int numberOfInstances, List<String> possibleImplementations, boolean forced, String comment) {
        super(serviceId, serviceImplementationId, comment);
        super.setForced(forced);
        this.numberOfInstances = numberOfInstances;
        this.possibleImplementations = possibleImplementations;
    }

    public ChangeImplementationOption(String serviceId, String serviceImplementationId, int numberOfInstances, List<String> possibleImplementations, Class<? extends QoSSpecification> goal, String comment) {
        super(serviceId, serviceImplementationId, comment);
        super.setQosGoal(goal);
        this.numberOfInstances = numberOfInstances;
        this.possibleImplementations = possibleImplementations;
    }


    @Override
    public String getDescription() {
        return (isForced() ? "FORCED" : ("Goal: " + getQosGoal().getSimpleName())) + " - Change "+super.getServiceId()+" implementation from "+super.getServiceImplementationId()+" to "+newImplementationId+". " + getComment();
    }
}
