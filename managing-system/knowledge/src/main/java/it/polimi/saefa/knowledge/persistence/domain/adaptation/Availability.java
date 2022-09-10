package it.polimi.saefa.knowledge.persistence.domain.adaptation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Availability extends AdaptationParameter{
    @JsonProperty("min_threshold")
    private double minThreshold;

    @Override
    public boolean isSatisfied() {
        return super.getValue()>=minThreshold;
    }

    public Availability(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {
        Gson gson = new Gson();
        JsonObject parameter = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        super.setPriority(parameter.get("priority").getAsInt());
        super.setWeight(parameter.get("weight").getAsDouble());
        minThreshold = parameter.get("min_threshold").getAsDouble();
    }

    @JsonCreator
    public Availability(@JsonProperty("value") Double value,
                        @JsonProperty("weight") Double weight,
                        @JsonProperty("priority") int priority,
                        @JsonProperty("min_threshold") Double min_threshold) {
        super(value, weight, priority);
        this.minThreshold = min_threshold;
    }

    public Double getThreshold() {
        return minThreshold;
    }

    @Override
    public String toString() {
        return super.toString() + ", Threshold: " + minThreshold + ")";
    }
}
