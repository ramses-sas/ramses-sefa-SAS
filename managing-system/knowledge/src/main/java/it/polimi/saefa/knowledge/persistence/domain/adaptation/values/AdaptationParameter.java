package it.polimi.saefa.knowledge.persistence.domain.adaptation.values;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Data;
import lombok.Getter;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Getter
public class AdaptationParameter<T extends AdaptationParamSpecification> {

    private final T specification;
    private final List<Value> valuesStack = new LinkedList<>();

    public AdaptationParameter(T specification) {
        this.specification = specification;
    }

    public void addValue(double value) {
        Value v = new Value(value, new Date());
        valuesStack.add(0, v);
    }

    @JsonIgnore
    public double getLastValue() {
        return valuesStack.get(0).getValue();
    }

    @JsonIgnore
    public Value getLastValueObject() {
        return valuesStack.get(0);
    }

    @JsonIgnore
    public boolean isCurrentlySatisfied() {
        return specification.isSatisfied(getLastValue());
    }

    @Data
    public static class Value {
        private double value;
        private Date timestamp;

        private Value(double value, Date timestamp){
            this.value = value;
            this.timestamp = timestamp;
        }
    }
}
