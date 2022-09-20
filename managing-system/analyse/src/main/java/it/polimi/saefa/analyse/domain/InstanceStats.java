package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceStats {
    private Instance instance;
    private Double averageResponseTime;
    private Double maxResponseTime;
    private Double availability;
    private boolean dataUnavailable = false;

    public InstanceStats(Instance instance, Double averageResponseTime, Double maxResponseTime, Double availability) {
        this.instance = instance;
        this.averageResponseTime = averageResponseTime;
        this.maxResponseTime = maxResponseTime;
        this.availability = availability;
    }

    public InstanceStats(Instance instance) {
        this.instance = instance;
        this.dataUnavailable = true;
    }

    public String getInstanceId() {
        return instance.getInstanceId();
    }
}
