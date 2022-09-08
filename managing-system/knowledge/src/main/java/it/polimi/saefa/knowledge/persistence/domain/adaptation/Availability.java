package it.polimi.saefa.knowledge.persistence.domain.adaptation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Availability extends AdaptationParameter{
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
}
