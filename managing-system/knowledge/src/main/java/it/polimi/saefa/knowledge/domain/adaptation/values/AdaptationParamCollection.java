package it.polimi.saefa.knowledge.domain.adaptation.values;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Data;

import java.util.*;

@Data
public class AdaptationParamCollection {
    private final Map<Class<? extends AdaptationParamSpecification>, AdaptationParameter<? extends AdaptationParamSpecification>> adaptationParamValueHistories = new HashMap<>();
    private final Map<Class<? extends AdaptationParamSpecification>, Double> adaptationParamBootBenchmark = new HashMap<>();

    public List<AdaptationParamSpecification> getAdaptationParameterSpecification(){
        List<AdaptationParamSpecification> toReturn = new LinkedList<>();
        adaptationParamValueHistories.values().forEach(
            adaptationParameter -> toReturn.add(adaptationParameter.getSpecification())
        );
        return toReturn;
    }

    @JsonIgnore
    public Double getBootBenchmark(Class<? extends AdaptationParamSpecification> adaptationParamSpecificationClass){
        return adaptationParamBootBenchmark.get(adaptationParamSpecificationClass);
    }

    public void setBootBenchmark(Class<? extends AdaptationParamSpecification> adaptationParamSpecificationClass, Double benchmark){
        adaptationParamBootBenchmark.put(adaptationParamSpecificationClass, benchmark);
    }

    @JsonIgnore
    public <T extends AdaptationParamSpecification> AdaptationParameter<T> getAdaptationParam(Class<T> adaptationParamClass) {
        return (AdaptationParameter<T>) adaptationParamValueHistories.get(adaptationParamClass);
    }

    public <T extends AdaptationParamSpecification> void addNewAdaptationParamValue(Class<T> adaptationParamClass, Double value) {
        AdaptationParameter<T> adaptationParameter = (AdaptationParameter<T>) adaptationParamValueHistories.get(adaptationParamClass);
        adaptationParameter.addValue(value);
    }

    @JsonIgnore
    public <T extends AdaptationParamSpecification> AdaptationParameter.Value getLatestAdaptationParamValue(Class<T> adaptationParamClass) {
        return adaptationParamValueHistories.get(adaptationParamClass).getLastValueObject();
    }

    @JsonIgnore
    public <T extends AdaptationParamSpecification> List<Double> getLatestNAdaptationParamValues(Class<T> adaptationParamClass, int n) {
        return adaptationParamValueHistories.get(adaptationParamClass).getLastNValues(n);
    }

    public <T extends AdaptationParamSpecification> void createHistory(T adaptationParam) {
        if (!adaptationParamValueHistories.containsKey(adaptationParam.getClass())) {
            AdaptationParameter<T> history = new AdaptationParameter<>(adaptationParam);
            adaptationParamValueHistories.put(adaptationParam.getClass(), history);
        }
    }

    @JsonIgnore
    public Collection<AdaptationParameter<? extends AdaptationParamSpecification>> getAdaptationParamHistories() {
        return adaptationParamValueHistories.values();
    }
}