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

    // We don't have enough metrics to compute the stats, but the instance is NOT a just born one: so we use the last valid stats
    public InstanceStats(Instance instance) {
        this.instance = instance;
        availability = instance.getAdaptationParamCollection().getAdaptationParam(Availability.class).getLastValue();
        averageResponseTime = instance.getAdaptationParamCollection().getAdaptationParam(AverageResponseTime.class).getLastValue();
        maxResponseTime = instance.getAdaptationParamCollection().getAdaptationParam(MaxResponseTime.class).getLastValue();
        this.fromNewData = false;
    }

    // We don't have enough metrics to compute the stats and the instance is JUST BORN: so we use the benchmarks provided by the architecture specification
    public InstanceStats(Instance instance, Map<Class<? extends AdaptationParamSpecification>, Double> bootBenchmarks) {
        this.instance = instance;
        availability = bootBenchmarks.get(Availability.class);
        averageResponseTime = bootBenchmarks.get(AverageResponseTime.class);
        maxResponseTime = bootBenchmarks.get(MaxResponseTime.class);
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