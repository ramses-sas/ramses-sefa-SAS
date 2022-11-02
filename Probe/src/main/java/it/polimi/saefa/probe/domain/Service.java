package it.polimi.saefa.probe.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.probe.configuration.ServiceConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class Service {
    private String serviceId;
    private String currentImplementationId;
    private List<String> instances;

    public Service(String serviceId) {
        this.serviceId = serviceId;
        this.instances = new LinkedList<>();
    }

    public void addInstance(String instanceId) {
        this.instances.add(instanceId);
    }
}
