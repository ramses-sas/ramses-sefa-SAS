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
@DiscriminatorValue("ADD_INSTANCES")
public class AddInstances extends AdaptationOption {
    private double newInstanceAvailabilityEstimation; //todo if oracle, not needed? Or still needed?
    private Double newInstanceAverageResponseTimeEstimation;
    private Integer numberOfInstancesToAdd;

    public AddInstances(String serviceId, String implementationId, double newInstanceAvailabilityEstimation, String comment) {
        super(serviceId, implementationId, comment);
        this.newInstanceAvailabilityEstimation = newInstanceAvailabilityEstimation;
    }

    public AddInstances(String serviceId, String implementationId, double newInstanceAvailabilityEstimation, String comment, boolean isForced) {
        super(serviceId, implementationId, comment);
        this.newInstanceAvailabilityEstimation = newInstanceAvailabilityEstimation;
        super.setForced(isForced);
    }

    @Override
    public String getDescription() {
        return isForced() ? "FORCED " : "" + "Add" + (numberOfInstancesToAdd == null ? "" : " "+numberOfInstancesToAdd) + " new instances of service " + super.getServiceId() + ". " + getComment();
    }

}
