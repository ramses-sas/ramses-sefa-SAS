package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.MaxResponseTime;
import it.polimi.saefa.knowledge.domain.architecture.Instance;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceStats {
    private Instance instance;
    private Double averageResponseTime;
    private Double maxResponseTime;
    private Double availability;
    private boolean newInstance;
    private boolean newStats;

    public InstanceStats(Instance instance, Double averageResponseTime, Double maxResponseTime, Double availability) {
        this.instance = instance;
        this.averageResponseTime = averageResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.availability = availability;
        newStats = true;
        newInstance = false;
    }

    public InstanceStats(Instance instance) {
        this.instance = instance;
        if (instance.getAdaptationParamCollection().existsEmptyHistory())
            this.newInstance = true;
        else {
            availability = instance.getAdaptationParamCollection().getAdaptationParam(Availability.class).getLastValue();
            averageResponseTime = instance.getAdaptationParamCollection().getAdaptationParam(AverageResponseTime.class).getLastValue();
            maxResponseTime = instance.getAdaptationParamCollection().getAdaptationParam(MaxResponseTime.class).getLastValue();
        }
        this.newStats = false;


    }

    public String getInstanceId() {
        return instance.getInstanceId();
    }

    public String getServiceId() {
        return instance.getServiceId();
    }

    public String getImplementationId() {
        return instance.getServiceImplementationId();
    }

    public boolean isNewInstance() {
        return newInstance || averageResponseTime == null || maxResponseTime == null || availability == null;
    }
}
