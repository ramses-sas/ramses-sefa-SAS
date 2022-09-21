package it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Availability extends AdaptationParamSpecification {
    @JsonProperty("min_threshold")
    private double minThreshold;
    @JsonIgnore
    private Double averageAvailability;

    @Override
    public boolean isSatisfied(double value) {
        return value>=minThreshold;
    }

    public Availability(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {
        Gson gson = new Gson();
        JsonObject parameter = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        //super.setPriority(parameter.get("priority").getAsInt());
        super.setWeight(parameter.get("weight").getAsDouble());
        minThreshold = parameter.get("min_threshold").getAsDouble();
    }

    @JsonCreator
    public Availability(
                        @JsonProperty("weight") Double weight,
                        //@JsonProperty("priority") int priority,
                        @JsonProperty("min_threshold") Double min_threshold) {
        //super(value, weight, priority);
        super(weight);

        this.minThreshold = min_threshold;
    }

    @Override
    public String getConstraintDescription() {
        return "value > "+minThreshold;
    }

    @Override
    public String toString() {
        return super.toString() + ", Constraint: " + getConstraintDescription() + ")";
    }

    public Double getAverageAvailability() {
        return averageAvailability;
    }

    public void setAverageAvailability(Double averageAvailability) {
        this.averageAvailability = averageAvailability;
    }
}
