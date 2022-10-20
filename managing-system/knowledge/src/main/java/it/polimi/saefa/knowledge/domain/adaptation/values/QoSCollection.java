package it.polimi.saefa.knowledge.domain.adaptation.values;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Data;

import java.util.*;

@Data
public class QoSCollection {
    private final Map<Class<? extends QoSSpecification>, QoSHistory<? extends QoSSpecification>> qoSHistoryMap = new HashMap<>();

    public <T extends QoSSpecification> QoSHistory<T> getQoSHistory(Class<T> qosClass) {
        return (QoSHistory<T>) qoSHistoryMap.get(qosClass);
    }


    // Functions on current value
    public QoSHistory.Value changeCurrentValueForQoS(Class<? extends QoSSpecification> qosSpecificationClass, double value) {
        qoSHistoryMap.get(qosSpecificationClass).setCurrentValue(new QoSHistory.Value(value, new Date()));
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
    public <T extends QoSSpecification> QoSHistory.Value createNewQoSValue(Class<T> qosClass, Double value) {
        QoSHistory<T> qoSHistory = (QoSHistory<T>) qoSHistoryMap.get(qosClass);
        return qoSHistory.addValue(value);
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


    public boolean existsEmptyHistory() {
        for (QoSHistory<? extends QoSSpecification> qoSHistory : qoSHistoryMap.values()) {
            if (qoSHistory.getValuesStack().isEmpty())
                return true;
        }
        return false;
    }

    public List<QoSSpecification> getAllQoSSpecifications() {
        List<QoSSpecification> toReturn = new LinkedList<>();
        qoSHistoryMap.values().forEach(
                qoSSpecifications -> toReturn.add(qoSSpecifications.getSpecification())
        );
        return toReturn;
    }

}
