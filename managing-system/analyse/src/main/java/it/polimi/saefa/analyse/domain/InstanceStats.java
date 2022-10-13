package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.MaxResponseTime;
import it.polimi.saefa.knowledge.domain.architecture.Instance;

import it.polimi.saefa.knowledge.domain.architecture.Service;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class InstanceStats {
    private Instance instance;
    private double averageResponseTime;
    private double maxResponseTime;
    private double availability;
    private boolean fromNewData;

    public InstanceStats(Instance instance, double averageResponseTime, double maxResponseTime, double availability) {
        this.instance = instance;
        this.averageResponseTime = averageResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.availability = availability;
        fromNewData = true;
    }

    // We don't have enough metrics to compute the stats, so we use the current value for each parameter
    public InstanceStats(Instance instance) {
        this.instance = instance;
        availability = instance.getCurrentValueForParam(Availability.class).getValue();
        averageResponseTime = instance.getCurrentValueForParam(AverageResponseTime.class).getValue();
        maxResponseTime = instance.getCurrentValueForParam(MaxResponseTime.class).getValue();
        this.fromNewData = false;
    }

}






/*

    public String getInstanceId() {
        return instance.getInstanceId();
    }

    public String getServiceId() {
        return instance.getServiceId();
    }

    public String getImplementationId() {
        return instance.getServiceImplementationId();
    }
 */