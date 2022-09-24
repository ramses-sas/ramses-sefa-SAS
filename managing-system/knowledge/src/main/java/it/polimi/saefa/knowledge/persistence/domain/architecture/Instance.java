package it.polimi.saefa.knowledge.persistence.domain.architecture;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.values.AdaptationParamCollection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Instance {
    private String instanceId; //service implementation name @ ip : port
    private String serviceId; //serviceId
    private String serviceImplementationId; //service implementation name
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;
    private AdaptationParamCollection adaptationParamCollection = new AdaptationParamCollection();

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
