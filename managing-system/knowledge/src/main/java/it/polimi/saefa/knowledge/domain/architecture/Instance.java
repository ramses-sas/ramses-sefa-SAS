package it.polimi.saefa.knowledge.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParameter;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetricsSnapshot;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Instance {
    private String instanceId; //service implementation id @ ip : port
    private String serviceId; //serviceId
    private String serviceImplementationId; //service implementation id
    private InstanceStatus currentStatus;
    private AdaptationParamCollection adaptationParamCollection;
    private InstanceMetricsSnapshot latestInstanceMetricsSnapshot;

    public Instance(String instanceId, String serviceId) {
        this.instanceId = instanceId;
        this.serviceImplementationId = instanceId.split("@")[0];
        this.serviceId = serviceId;
        currentStatus = InstanceStatus.BOOTING;
        latestInstanceMetricsSnapshot = new InstanceMetricsSnapshot(serviceId, instanceId);
        latestInstanceMetricsSnapshot.setStatus(currentStatus);
        latestInstanceMetricsSnapshot.applyTimestamp();
        adaptationParamCollection = new AdaptationParamCollection();
    }

    public <T extends AdaptationParamSpecification> List<Double> getLatestFilledAnalysisWindowForParam(Class<T> adaptationParamClass, int n) {
        return getAdaptationParamCollection().getLatestAnalysisWindowForParam(adaptationParamClass, n, true);
    }

    public <T extends AdaptationParamSpecification> AdaptationParameter.Value getCurrentValueForParam(Class<T> adaptationParamClass) {
        return getAdaptationParamCollection().getCurrentValueForParam(adaptationParamClass);
    }

    public <T extends AdaptationParamSpecification> void changeCurrentValueForParam(Class<T> adaptationParamClass, double newValue) {
        getAdaptationParamCollection().changeCurrentValueForParam(adaptationParamClass, newValue);
    }

    public <T extends AdaptationParamSpecification> void invalidateAdaptationParametersHistory(Class<T> adaptationParamClass) {
        getAdaptationParamCollection().invalidateLatestAndPreviousValuesForParam(adaptationParamClass);
    }

    @JsonIgnore
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
