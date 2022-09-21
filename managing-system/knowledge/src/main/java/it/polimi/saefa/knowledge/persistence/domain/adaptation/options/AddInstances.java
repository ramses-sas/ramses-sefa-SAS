package it.polimi.saefa.knowledge.persistence.domain.adaptation.options;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;

public class AddInstances extends AdaptationOption {


    public AddInstances(Service service) {
        super(service);
    }

    @Override
    public String getDescription() {
        return "Add new instances of service " + super.getService().getServiceId();
    }

}
