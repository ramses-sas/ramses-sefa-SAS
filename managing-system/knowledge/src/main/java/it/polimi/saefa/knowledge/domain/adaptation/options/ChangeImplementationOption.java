package it.polimi.saefa.knowledge.domain.adaptation.options;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.List;

@Data
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

    public ChangeImplementationOption(String serviceId, String serviceImplementationId, int numberOfInstances, List<String> possibleImplementations, Class<? extends AdaptationParamSpecification> goal, String comment) {
        super(serviceId, serviceImplementationId, comment);
        super.setAdaptationParametersGoal(goal);
        this.numberOfInstances = numberOfInstances;
        this.possibleImplementations = possibleImplementations;
    }


    @Override
    public String getDescription() {
        return (isForced() ? "FORCED" : ("Goal: " + getAdaptationParametersGoal().getSimpleName())) + " - Change implementation of service " + super.getServiceId() + ".\n" + getComment();
    }
}
