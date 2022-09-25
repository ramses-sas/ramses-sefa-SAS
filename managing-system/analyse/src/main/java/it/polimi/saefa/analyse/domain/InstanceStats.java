package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InstanceStats {
    private Instance instance;
    private Double averageResponseTime;
    private Double maxResponseTime;
    private Double availability;

    public String getInstanceId() {
        return instance.getInstanceId();
    }
}
