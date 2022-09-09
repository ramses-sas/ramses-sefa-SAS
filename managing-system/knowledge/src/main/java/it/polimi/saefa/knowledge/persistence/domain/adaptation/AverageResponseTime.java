package it.polimi.saefa.knowledge.persistence.domain.adaptation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AverageResponseTime extends AdaptationParameter{
    @JsonProperty("max_threshold")
    private double maxThreshold;

    @Override
    public boolean isSatisfied() {
        return super.getValue()<= maxThreshold;
    }

    public AverageResponseTime(String json) {
        super(json);
    }

    @Override
    public void parseFromJson(String json) {
        Gson gson = new Gson();
        JsonObject parameter = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        super.setPriority(parameter.get("priority").getAsInt());
        super.setWeight(parameter.get("weight").getAsDouble());
        maxThreshold = parameter.get("max_threshold").getAsDouble();
    }

    @JsonCreator
    public AverageResponseTime(
            @JsonProperty("value") Double value,
            @JsonProperty("weight") Double weight,
            @JsonProperty("priority") int priority,
            @JsonProperty("max_threshold") Double max_threshold) {
        super(value, weight, priority);
        this.maxThreshold = max_threshold;
    }

    @Override
    public String toString() {
        return super.toString() + ", Threshold: " + maxThreshold + ")";
    }
}