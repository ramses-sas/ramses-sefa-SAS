package it.polimi.ramses.knowledge.domain.adaptation.values;

import com.fasterxml.jackson.annotation.*;
import it.polimi.ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class QoSHistory<T extends QoSSpecification> {

    private T specification;
    private List<Value> valuesStack = new LinkedList<>();
    private QoSHistory.Value currentValue;

    public QoSHistory(T specification) {
        this.specification = specification;
    }

    public Value addValue(double value, Date date) {
        valuesStack.add(0, new Value(value, date));
        return valuesStack.get(0);
    }


    @JsonIgnore
    public Value getLatestValue() {
        if (valuesStack.size() > 0)
            return valuesStack.get(0);
        return null;
    }

    // Get the latest "size" VALID values from the valueStack. If there are less than "size" VALID values, returns NULL
    public List<Double> getLatestAnalysisWindow(int size) {
        List<Double> values = new LinkedList<>();
        if (valuesStack.size() < size)
            return null;
        for (int i = 0; i < size; i++) {
            if (!valuesStack.get(i).invalidatesThisAndPreviousValues())
                values.add(valuesStack.get(i).getDoubleValue());
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
        int finalSize = Math.min(size, valuesStack.size());
        for (int i = 0; i < finalSize; i++) {
            if (!valuesStack.get(i).invalidatesThisAndPreviousValues())
                values.add(valuesStack.get(i).getDoubleValue());
            else
                break;
        }
        while (values.size() < size)
            values.add(currentValue.getDoubleValue());
        return values;
    }

    public void invalidateLatestAndPreviousValues() {
        if (valuesStack.size() > 0)
            valuesStack.get(0).invalidateThisAndPreviousValues();
    }

    @Data
    public static class Value {
        private boolean invalidatesThisAndPreviousValues = false;
        private final double doubleValue;
        private final Date timestamp;

        protected Value(double doubleValue, Date timestamp) {
            this.doubleValue = doubleValue;
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
            return String.format("%.3f", doubleValue);
        }
    }
}
