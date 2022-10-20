package it.polimi.saefa.knowledge.domain.architecture;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSCollection;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSHistory;
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
    private QoSCollection qoSCollection;
    private InstanceMetricsSnapshot latestInstanceMetricsSnapshot;

    public Instance(String instanceId, String serviceId) {
        this.instanceId = instanceId;
        this.serviceImplementationId = instanceId.split("@")[0];
        this.serviceId = serviceId;
        currentStatus = InstanceStatus.BOOTING;
        latestInstanceMetricsSnapshot = new InstanceMetricsSnapshot(serviceId, instanceId);
        latestInstanceMetricsSnapshot.setStatus(currentStatus);
        latestInstanceMetricsSnapshot.applyTimestamp();
        qoSCollection = new QoSCollection();
    }

    public <T extends QoSSpecification> List<Double> getLatestFilledAnalysisWindowForQoS(Class<T> qoSClass, int n) {
        return getQoSCollection().getLatestAnalysisWindowForQoS(qoSClass, n, true);
    }

    public <T extends QoSSpecification> QoSHistory.Value getCurrentValueForQoS(Class<T> qoSClass) {
        return getQoSCollection().getCurrentValueForQoS(qoSClass);
    }

    public <T extends QoSSpecification> void changeCurrentValueForQoS(Class<T> qoSClass, double newValue) {
        getQoSCollection().changeCurrentValueForQoS(qoSClass, newValue);
    }

    public <T extends QoSSpecification> void invalidateQoSHistory(Class<T> qoSClass) {
        getQoSCollection().invalidateLatestAndPreviousValuesForQoS(qoSClass);
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
