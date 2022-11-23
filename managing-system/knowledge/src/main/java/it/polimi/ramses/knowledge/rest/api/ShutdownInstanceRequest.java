package it.polimi.ramses.knowledge.rest.api;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ShutdownInstanceRequest {
    private String serviceId;
    private String instanceId;

    public ShutdownInstanceRequest(String serviceId, String instanceId) {
        this.serviceId = serviceId;
        this.instanceId = instanceId;
    }
}
