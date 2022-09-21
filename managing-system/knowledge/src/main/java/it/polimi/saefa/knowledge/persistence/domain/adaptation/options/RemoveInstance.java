package it.polimi.saefa.knowledge.persistence.domain.adaptation.options;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;

public class RemoveInstance extends AdaptationOption {

    private final Instance instance;

    public RemoveInstance(Service service, Instance instance) {
        super(service);
        this.instance = instance;
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public String getDescription() {
        return "Remove instance " + instance.getInstanceId() + " of service " + super.getService().getServiceId();
    }
}

