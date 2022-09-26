package it.polimi.saefa.knowledge.persistence.domain.adaptation.options;

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
    private double newInstanceAvailabilityEstimation;

    public AddInstances(String serviceId, String implementationId, double newInstanceAvailabilityEstimation) {
        super(serviceId, implementationId);
        this.newInstanceAvailabilityEstimation = newInstanceAvailabilityEstimation;
    }

    @Override
    public String getDescription() {
        return "Add new instances of service " + super.getServiceId();
    }

}
