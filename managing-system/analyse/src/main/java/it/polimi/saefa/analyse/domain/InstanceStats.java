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

    public InstanceStats(Instance instance) {
        this.instance = instance;
        if (instance.getAdaptationParamCollection().existsEmptyHistory()) { // if the instance is just born we use the values provided by the architecture specification
            availability = instance.getAdaptationParamCollection().getBootBenchmark(Availability.class);
            averageResponseTime = instance.getAdaptationParamCollection().getBootBenchmark(AverageResponseTime.class);
            maxResponseTime = instance.getAdaptationParamCollection().getBootBenchmark(MaxResponseTime.class);
        } else { // otherwise we use the last values provided by the latest adaptation
            availability = instance.getAdaptationParamCollection().getAdaptationParam(Availability.class).getLastValue();
            averageResponseTime = instance.getAdaptationParamCollection().getAdaptationParam(AverageResponseTime.class).getLastValue();
            maxResponseTime = instance.getAdaptationParamCollection().getAdaptationParam(MaxResponseTime.class).getLastValue();
        }
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