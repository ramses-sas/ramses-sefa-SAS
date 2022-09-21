package it.polimi.saefa.knowledge.persistence.domain.adaptation.values;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AdaptationParamCollection{
    private final Map<Class<? extends AdaptationParamSpecification>, AdaptationParameter<? extends AdaptationParamSpecification>> adaptationParamValueHistories = new HashMap<>();

    @JsonIgnore
    public  <T extends AdaptationParamSpecification> AdaptationParameter<T> getAdaptationParam(Class<T> adaptationParamClass) {
        return (AdaptationParameter<T>) adaptationParamValueHistories.get(adaptationParamClass);
    }

    public <T extends AdaptationParamSpecification> void addNewAdaptationParamValue(Class<T> adaptationParamClass, Double value) {
        AdaptationParameter<T> adaptationParameter = (AdaptationParameter<T>) adaptationParamValueHistories.get(adaptationParamClass);
        adaptationParameter.addValue(value);
    }
    @JsonIgnore
    public  <T extends AdaptationParamSpecification> AdaptationParameter.Value getLatestAdaptationParamValue(Class<T> adaptationParamClass) {
        return adaptationParamValueHistories.get(adaptationParamClass).getLastValueObject();
    }

    public <T extends AdaptationParamSpecification> void createHistory(T adaptationParam){
        if(!adaptationParamValueHistories.containsKey(adaptationParam.getClass())) {
            AdaptationParameter<T> history = new AdaptationParameter<>(adaptationParam);
            adaptationParamValueHistories.put(adaptationParam.getClass(), history);
        }
    }
    @JsonIgnore
    public Collection<AdaptationParameter<? extends AdaptationParamSpecification>> getAdaptationParamHistories() {
        return adaptationParamValueHistories.values();
    }
}
