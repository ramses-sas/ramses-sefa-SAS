package it.polimi.saefa.knowledge.rest;

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
