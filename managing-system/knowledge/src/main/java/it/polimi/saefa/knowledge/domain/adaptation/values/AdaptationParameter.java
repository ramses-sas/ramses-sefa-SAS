package it.polimi.saefa.knowledge.domain.adaptation.values;

import com.fasterxml.jackson.annotation.*;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class AdaptationParameter<T extends AdaptationParamSpecification> {

    private T specification;
    private List<Value> valuesStack = new LinkedList<>();

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

    // Get the latest "size" VALID values from the valueStack. If "replicateLastValue" is true, the last value is replicated
    // until the size is reached, even if invalid. If "replicateLastValue" is false, the last value is not replicated.
    // The method returns null if the valueStack is empty or if "replicateLastValue" is false and there are less than "size" VALID values.
    @JsonIgnore
    public List<Double> getLatestFilledAnalysisWindow(int size) {
        List<Double> values = new LinkedList<>();
        int i = Math.min(valuesStack.size(), size) - 1;
        int validValues = 0;
        while (validValues < size) {
            if (i < 0) {
                return null;
            } else {
                Value v = valuesStack.get(i);
                if (!v.invalidatesThisAndPreviousValues) {
                    values.add(v.getValue());
                    validValues++;
                }
                i--;
            }
        }
        return values;
    }

    @JsonIgnore
    public List<Double> getLatestFilledAnalysisWindow(int size, double currentValue) {
        List<Double> values = new LinkedList<>();
        int i = Math.min(valuesStack.size(), size) - 1;
        int validValues = 0;
        while (validValues < size) {
            if (i < 0) {
                values.add(currentValue);
                validValues++;
            } else {
                Value v = valuesStack.get(i);
                if (!v.invalidatesThisAndPreviousValues) {
                    values.add(v.getValue());
                    validValues++;
                }
                i--;
            }
        }
        return values;
    }

    public void invalidateLatestAndPreviousValues() {
        if (valuesStack.size() > 0)
            valuesStack.get(0).invalidatesThisAndPreviousValues = true;
    }

    @Data
    public static class Value {
        private boolean invalidatesThisAndPreviousValues = false;
        private double value;
        private Date timestamp;

        public Value(double value, Date timestamp){
            this.value = value;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("%.3f", value);
        }
    }


    /*
    @JsonIgnore
    public List<Double> getLastNValues(int n) {
        List<Double> values = null;
        if (valuesStack.size() >= n) {
            values = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                values.add(valuesStack.get(i).getValue());
        }
        return values;
    }

    @JsonIgnore
    public Value getLastValueObject() {
        if (valuesStack.size() > 0)
            return valuesStack.get(0);
        return null;
    }

    @JsonIgnore
    public boolean isCurrentlySatisfied() {
        return specification.isSatisfied(getLastValue());
    }

    @JsonIgnore
    public boolean isSatisfiedByPercentage(int n, double percentage) {
        return specification.isSatisfied(getLastNValues(n), percentage);
    }
    */
}
