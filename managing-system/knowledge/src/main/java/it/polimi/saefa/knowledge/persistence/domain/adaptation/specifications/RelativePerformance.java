package it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class RelativePerformance extends AdaptationParamSpecification{
    @JsonProperty("min_threshold")
    private double minThreshold;

    @JsonCreator
    public RelativePerformance() { super(); }

    public RelativePerformance(String json) {
        super();
        fromJson(json);
    }

    @Override
    void fromJson(String json) {
        Gson gson = new Gson();
        JsonObject parameter = gson.fromJson(json, JsonObject.class).getAsJsonObject();
        setWeight(parameter.get("weight").getAsDouble());
        minThreshold = parameter.get("min_threshold").getAsDouble();
    }

    @Override
    public String getConstraintDescription() {
        return "value > "+ minThreshold;
    }

    @Override
    @JsonIgnore
    public boolean isSatisfied(double value) {
        return value >= minThreshold;
    }

}
