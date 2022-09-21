package it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications;

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
public abstract class AdaptationParamSpecification {


    private Double weight;
    //private int priority;

    public AdaptationParamSpecification(String json) {
        parseFromJson(json);
    }

    public abstract boolean isSatisfied(double value);

    public abstract String getConstraintDescription();

    public abstract void parseFromJson(String json);



    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(weight: " + weight; //+ ", priority: " + priority;
    }




}
