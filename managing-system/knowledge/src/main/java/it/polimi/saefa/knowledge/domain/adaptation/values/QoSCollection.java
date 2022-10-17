package it.polimi.saefa.knowledge.domain.adaptation.values;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Data;

import java.util.*;

@Data
public class QoSCollection {
    private final Map<Class<? extends QoSSpecification>, QoSHistory<? extends QoSSpecification>> qoSHistoryMap = new HashMap<>();

    public <T extends QoSSpecification> QoSHistory<T> getQoSHistory(Class<T> adaptationParamClass) {
        return (QoSHistory<T>) qoSHistoryMap.get(adaptationParamClass);
    }


    // Functions on current value
    public void changeCurrentValueForQoS(Class<? extends QoSSpecification> adaptationParamSpecificationClass, double value) {
        qoSHistoryMap.get(adaptationParamSpecificationClass).setCurrentValue(new QoSHistory.Value(value, new Date()));
    }

    public QoSHistory.Value getCurrentValueForQoS(Class<? extends QoSSpecification> adaptationParamSpecificationClass) {
        return qoSHistoryMap.get(adaptationParamSpecificationClass).getCurrentValue();
    }

    public void invalidateLatestAndPreviousValuesForQoS(Class<? extends QoSSpecification> adaptationParamSpecificationClass) {
        qoSHistoryMap.get(adaptationParamSpecificationClass).invalidateLatestAndPreviousValues();
    }


    // Functions on values history
    public <T extends QoSSpecification> void addNewQoSValue(Class<T> adaptationParamClass, Double value) {
        QoSHistory<T> qoSHistory = (QoSHistory<T>) qoSHistoryMap.get(adaptationParamClass);
        qoSHistory.addValue(value);
    }

    public <T extends QoSSpecification> List<Double> getLatestAnalysisWindowForQoS(Class<T> adaptationParamClass, int windowSize, boolean fillWithCurrentValue) {
        return fillWithCurrentValue ?
                qoSHistoryMap.get(adaptationParamClass).getLatestFilledAnalysisWindow(windowSize) :
                qoSHistoryMap.get(adaptationParamClass).getLatestAnalysisWindow(windowSize);
    }

    public <T extends QoSSpecification> void createHistory(T adaptationParam) {
        if (!qoSHistoryMap.containsKey(adaptationParam.getClass())) {
            QoSHistory<T> history = new QoSHistory<>(adaptationParam);
            qoSHistoryMap.put(adaptationParam.getClass(), history);
        }
    }

    public <T extends QoSSpecification> List<QoSHistory.Value> getValuesHistoryForQoS(Class<T> adaptationParamClass) {
        return qoSHistoryMap.get(adaptationParamClass).getValuesStack();
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
                adaptationParameter -> toReturn.add(adaptationParameter.getSpecification())
        );
        return toReturn;
    }

}
