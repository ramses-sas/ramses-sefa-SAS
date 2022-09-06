package it.polimi.saefa.knowledge.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstanceInfoRequest {
    private String serviceId;
    private String instanceId;

    @Override
    public String toString() {
        return serviceId + "@" + instanceId;
    }
}
