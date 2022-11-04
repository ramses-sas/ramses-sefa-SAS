package it.polimi.saefa.knowledge.externalinterfaces;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class ServiceInfo {
    private String serviceId;
    private String currentImplementationId;
    private List<String> instances;

    public ServiceInfo(String serviceId) {
        this.serviceId = serviceId;
        this.instances = new LinkedList<>();
    }

    public void addInstance(String instanceId) {
        this.instances.add(instanceId);
    }
}
