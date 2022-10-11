package it.polimi.saefa.knowledge.domain.adaptation.values;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Data;

import java.util.*;

@Data
public class AdaptationParamCollection {
    private final Map<Class<? extends AdaptationParamSpecification>, AdaptationParameter<? extends AdaptationParamSpecification>> adaptationParamsMap = new HashMap<>();
    //private final Map<Class<? extends AdaptationParamSpecification>, AdaptationParameter.Value> currentAdaptationParamsValues = new HashMap<>();


    public <T extends AdaptationParamSpecification> AdaptationParameter<T> getAdaptationParam(Class<T> adaptationParamClass) {
        return (AdaptationParameter<T>) adaptationParamsMap.get(adaptationParamClass);
    }


    // Functions on current value
    public void changeCurrentValueForParam(Class<? extends AdaptationParamSpecification> adaptationParamSpecificationClass, double value) {
        adaptationParamsMap.get(adaptationParamSpecificationClass).setCurrentValue(new AdaptationParameter.Value(value, new Date()));
    }

    public AdaptationParameter.Value getCurrentValueForParam(Class<? extends AdaptationParamSpecification> adaptationParamSpecificationClass) {
        return adaptationParamsMap.get(adaptationParamSpecificationClass).getCurrentValue();
    }

    public void invalidateLatestAndPreviousValuesForParam(Class<? extends AdaptationParamSpecification> adaptationParamSpecificationClass) {
        adaptationParamsMap.get(adaptationParamSpecificationClass).invalidateLatestAndPreviousValues();
    }


    // Functions on values history
    public <T extends AdaptationParamSpecification> void addNewAdaptationParamValue(Class<T> adaptationParamClass, Double value) {
        AdaptationParameter<T> adaptationParameter = (AdaptationParameter<T>) adaptationParamsMap.get(adaptationParamClass);
        adaptationParameter.addValue(value);
    }

    public <T extends AdaptationParamSpecification> List<Double> getLatestAnalysisWindowForParam(Class<T> adaptationParamClass, int windowSize, boolean fillWithCurrentValue) {
        return fillWithCurrentValue ?
                adaptationParamsMap.get(adaptationParamClass).getLatestFilledAnalysisWindow(windowSize, getCurrentValueForParam(adaptationParamClass).getValue()) :
                adaptationParamsMap.get(adaptationParamClass).getLatestFilledAnalysisWindow(windowSize);
    }

    public <T extends AdaptationParamSpecification> void createHistory(T adaptationParam) {
        if (!adaptationParamsMap.containsKey(adaptationParam.getClass())) {
            AdaptationParameter<T> history = new AdaptationParameter<>(adaptationParam);
            adaptationParamsMap.put(adaptationParam.getClass(), history);
        }
    }

    public <T extends AdaptationParamSpecification> List<AdaptationParameter.Value> getValuesHistoryForParam(Class<T> adaptationParamClass) {
        return adaptationParamsMap.get(adaptationParamClass).getValuesStack();
    }


    public boolean existsEmptyHistory() {
        for (AdaptationParameter<? extends AdaptationParamSpecification> adaptationParameter : adaptationParamsMap.values()) {
            if (adaptationParameter.getValuesStack().isEmpty())
                return true;
        }
        return false;
    }

    public List<AdaptationParamSpecification> getAdaptationParametersSpecifications() {
        List<AdaptationParamSpecification> toReturn = new LinkedList<>();
        adaptationParamsMap.values().forEach(
                adaptationParameter -> toReturn.add(adaptationParameter.getSpecification())
        );
        return toReturn;
    }

}
