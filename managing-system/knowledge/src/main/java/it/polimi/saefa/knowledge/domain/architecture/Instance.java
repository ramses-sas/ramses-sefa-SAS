package it.polimi.saefa.knowledge.domain.architecture;

import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParamCollection;
import it.polimi.saefa.knowledge.domain.metrics.InstanceMetrics;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Instance {
    private String instanceId; //service implementation id @ ip : port
    private String serviceId; //serviceId
    private String serviceImplementationId; //service implementation id
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;
    private AdaptationParamCollection adaptationParamCollection = new AdaptationParamCollection();
    private InstanceMetrics lastMetrics;

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

    public String getAddress(){
        return instanceId.split("@")[1];
    }

}
