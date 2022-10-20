package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.Availability;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AverageResponseTime;
import it.polimi.saefa.knowledge.domain.architecture.Instance;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class InstanceStats {
    private Instance instance;
    private double averageResponseTime;
    //private double maxResponseTime;
    private double availability;
    private boolean fromNewData;

    public InstanceStats(Instance instance, double averageResponseTime, double availability) {
        this.instance = instance;
        this.averageResponseTime = averageResponseTime;
        //this.maxResponseTime = maxResponseTime;
        this.availability = availability;
        fromNewData = true;
    }

    // We don't have enough metrics to compute the stats, so we use the current value for each QoS
    public InstanceStats(Instance instance) {
        this.instance = instance;
        availability = instance.getCurrentValueForQoS(Availability.class).getDoubleValue();
        averageResponseTime = instance.getCurrentValueForQoS(AverageResponseTime.class).getDoubleValue();
        //maxResponseTime = instance.getCurrentValueForQoS(MaxResponseTime.class).getValue();
        this.fromNewData = false;
    }

}
