package it.polimi.ramses.knowledge.domain.adaptation.values;

import it.polimi.ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Data;

import java.util.*;

@Data
public class QoSCollection {
    private final Map<Class<? extends QoSSpecification>, QoSHistory<? extends QoSSpecification>> qoSHistoryMap = new HashMap<>();

    public <T extends QoSSpecification> QoSHistory<T> getQoSHistory(Class<T> qosClass) {
        return (QoSHistory<T>) qoSHistoryMap.get(qosClass);
    }

    // Functions on current value
    public QoSHistory.Value changeCurrentValueForQoS(Class<? extends QoSSpecification> qosSpecificationClass, double value, Date date) {
        qoSHistoryMap.get(qosSpecificationClass).setCurrentValue(new QoSHistory.Value(value, date));
        return qoSHistoryMap.get(qosSpecificationClass).getCurrentValue();
    }

    public void setCurrentValueForQoS(Class<? extends QoSSpecification> qosSpecificationClass, QoSHistory.Value value) {
        qoSHistoryMap.get(qosSpecificationClass).setCurrentValue(value);
    }

    public QoSHistory.Value getCurrentValueForQoS(Class<? extends QoSSpecification> qosSpecificationClass) {
        return qoSHistoryMap.get(qosSpecificationClass).getCurrentValue();
    }

    public void invalidateLatestAndPreviousValuesForQoS(Class<? extends QoSSpecification> qosSpecificationClass) {
        qoSHistoryMap.get(qosSpecificationClass).invalidateLatestAndPreviousValues();
    }


    // Functions on values history
    public <T extends QoSSpecification> QoSHistory.Value createNewQoSValue(Class<T> qosClass, double value, Date date) {
        QoSHistory<T> qoSHistory = (QoSHistory<T>) qoSHistoryMap.get(qosClass);
        return qoSHistory.addValue(value, date);
    }

    public <T extends QoSSpecification> void addNewQoSValue(Class<T> qosClass, QoSHistory.Value value) {
        QoSHistory<T> qoSHistory = (QoSHistory<T>) qoSHistoryMap.get(qosClass);
        qoSHistory.getValuesStack().add(0, value);
    }

    public <T extends QoSSpecification> List<Double> getLatestAnalysisWindowForQoS(Class<T> qosClass, int windowSize, boolean fillWithCurrentValue) {
        return fillWithCurrentValue ?
                qoSHistoryMap.get(qosClass).getLatestFilledAnalysisWindow(windowSize) :
                qoSHistoryMap.get(qosClass).getLatestAnalysisWindow(windowSize);
    }

    public <T extends QoSSpecification> void createHistory(T qos) {
        if (!qoSHistoryMap.containsKey(qos.getClass())) {
            QoSHistory<T> history = new QoSHistory<>(qos);
            qoSHistoryMap.put(qos.getClass(), history);
        }
    }

    public <T extends QoSSpecification> List<QoSHistory.Value> getValuesHistoryForQoS(Class<T> qosClass) {
        return qoSHistoryMap.get(qosClass).getValuesStack();
    }
}
