package it.polimi.saefa.knowledge.persistence.domain.adaptation.parameters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Availability.class),
        @JsonSubTypes.Type(value = AverageResponseTime.class),
        @JsonSubTypes.Type(value = MaxResponseTime.class),
        @JsonSubTypes.Type(value = TotalCost.class)
})
public abstract class AdaptationParameter {
    private Double value = 0d;
    private Double weight;
    //private int priority;

    public AdaptationParameter(String json) {
        parseFromJson(json);
    }

    public abstract boolean isSatisfied();

    public abstract Double getThreshold();

    public abstract void parseFromJson(String json);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(value: " + value + ", weight: " + weight; //+ ", priority: " + priority;
    }



}
