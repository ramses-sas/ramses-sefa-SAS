package it.polimi.saefa.knowledge.domain.adaptation.values;

import com.fasterxml.jackson.annotation.*;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class AdaptationParameter<T extends AdaptationParamSpecification> {

    private T specification;
    private List<Value> valuesStack = new LinkedList<>();
    private AdaptationParameter.Value currentValue;

    public AdaptationParameter( T specification) {
        this.specification = specification;
    }

    public void addValue(double value) {
        valuesStack.add(0, new Value(value, new Date()));
    }


    @JsonIgnore
    public Double getLastValue() {
        if (valuesStack.size() > 0)
            return valuesStack.get(0).getValue();
        return null;
    }

    // Get the latest "size" VALID values from the valueStack. If there are less than "size" VALID values, returns NULL
    public List<Double> getLatestAnalysisWindow(int size) {
        List<Double> values = new LinkedList<>();
        if (valuesStack.size() < size)
            return null;
        for (int i = 0; i < size; i++) {
            if (!valuesStack.get(i).invalidatesThisAndPreviousValues())
                values.add(valuesStack.get(i).getValue());
            else
                break;
        }
        if (values.size() < size)
            return null;
        return values;
    }

    // Get the latest "size" VALID values from the valueStack. If there are less than "size" VALID values, the current value is replicated
    public List<Double> getLatestFilledAnalysisWindow(int size) {
        List<Double> values = new LinkedList<>();
        if (valuesStack.size() < size)
            return null;
        for (int i = 0; i < size; i++) {
            if (!valuesStack.get(i).invalidatesThisAndPreviousValues())
                values.add(valuesStack.get(i).getValue());
            else
                break;
        }
        while (values.size() < size)
            values.add(currentValue.getValue());
        return values;
    }

    public void invalidateLatestAndPreviousValues() {
        if (valuesStack.size() > 0)
            valuesStack.get(0).invalidateThisAndPreviousValues();
    }


    public static class Value {
        private boolean invalidatesThisAndPreviousValues = false;
        @Getter
        private final double value;
        @Getter
        private final Date timestamp;

        public Value(double value, Date timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public boolean invalidatesThisAndPreviousValues() {
            return invalidatesThisAndPreviousValues;
        }

        public void invalidateThisAndPreviousValues() {
            this.invalidatesThisAndPreviousValues = true;
        }

        @Override
        public String toString() {
            return String.format("%.3f", value);
        }
    }
}
