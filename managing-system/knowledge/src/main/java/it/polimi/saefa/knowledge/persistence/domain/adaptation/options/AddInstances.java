package it.polimi.saefa.knowledge.persistence.domain.adaptation.options;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddInstances extends AdaptationOption {
    private double newInstanceAvailabilityEstimation;

    public AddInstances(Service service, double newInstanceAvailabilityEstimation) {
        super(service);
        this.newInstanceAvailabilityEstimation = newInstanceAvailabilityEstimation;
    }

    @Override
    public String getDescription() {
        return "Add new instances of service " + super.getService().getServiceId();
    }

}
