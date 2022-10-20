package it.polimi.saefa.knowledge.domain.adaptation.specifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class Availability extends QoSSpecification {
    @JsonProperty("min_threshold")
    private double minThreshold;
    //@JsonIgnore
    //private Double averageAvailability;


    @JsonCreator
    public Availability() { super(); }

    // used in QoSParser: clazz.getDeclaredConstructor(String.class)
    public Availability(String json) {
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

    /*
    @JsonCreator
    public Availability(
                        @JsonProperty("weight") Double weight,
                        //@JsonProperty("priority") int priority,
                        @JsonProperty("min_threshold") Double min_threshold) {
        //super(value, weight, priority);
        super(weight);

        this.minThreshold = min_threshold;
    }
    */
}
