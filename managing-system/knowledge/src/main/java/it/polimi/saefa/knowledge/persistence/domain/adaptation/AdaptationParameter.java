package it.polimi.saefa.knowledge.persistence.domain.adaptation;

import lombok.Data;
import lombok.Getter;

@Data
public abstract class AdaptationParameter {
    private Double value = 0d;
    private Double weight;
    private int priority;

    public AdaptationParameter(String json) {
        parseFromJson(json);
    }

    public AdaptationParameter() {
    }

    public abstract boolean isSatisfied();

    public abstract void parseFromJson(String json);



}
