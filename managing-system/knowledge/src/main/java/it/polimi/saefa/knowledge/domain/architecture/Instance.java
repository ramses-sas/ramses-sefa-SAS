package it.polimi.saefa.knowledge.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParameter;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Instance {
    private String instanceId; //service implementation id @ ip : port
    private String serviceId; //serviceId
    private String serviceImplementationId; //service implementation id
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;
    private AdaptationParamCollection adaptationParamCollection = new AdaptationParamCollection();
    private InstanceMetrics latestMetrics;

    public Instance(String instanceId, String serviceId) {
        this.instanceId = instanceId;
        this.serviceImplementationId = instanceId.split("@")[0];
        this.serviceId = serviceId;
    }

    public Instance(String instanceId, String serviceId, InstanceStatus currentStatus) {
        this.instanceId = instanceId;
        this.serviceImplementationId = instanceId.split("@")[0];
        this.serviceId = serviceId;
        this.currentStatus = currentStatus;
    }

    @JsonIgnore
    public <T extends AdaptationParamSpecification> List<Double> getLatestReplicatedAnalysisWindowForParam(Class<T> adaptationParamClass, int n) {
        return getAdaptationParamCollection().getLatestAnalysisWindowForParam(adaptationParamClass, n, true);
    }

    @JsonIgnore
    public <T extends AdaptationParamSpecification> AdaptationParameter.Value getCurrentValueForParam(Class<T> adaptationParamClass) {
        return getAdaptationParamCollection().getCurrentValueForParam(adaptationParamClass);
    }

    public <T extends AdaptationParamSpecification> void changeCurrentValueForParam(Class<T> adaptationParamClass, double newValue) {
        getAdaptationParamCollection().changeCurrentValueForParam(adaptationParamClass, newValue);
    }

    public <T extends AdaptationParamSpecification> void invalidateLatestAndPreviousValuesForParam(Class<T> adaptationParamClass) {
        getAdaptationParamCollection().invalidateLatestAndPreviousValuesForParam(adaptationParamClass);
    }

    public String getAddress(){
        return instanceId.split("@")[1];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instance instance = (Instance) o;
        return instanceId.equals(instance.instanceId);
    }

}
