package it.polimi.saefa.knowledge.persistence.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class Instance{
    private String instanceId; //service implementation name @ ip : port
    private Service service;
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;

    private List<InstanceMetrics> metrics = new LinkedList<>();

    public Instance(String instanceId, Service service) {
        this.instanceId = instanceId;
        this.service = service;
    }

    public Instance(String instanceId, Service service, InstanceStatus currentStatus) {
        this.instanceId = instanceId;
        this.service = service;
        this.currentStatus = currentStatus;
    }

    public void addMetric(InstanceMetrics metric){
        metrics.add(metric);
    }

    public String getAddress(){
        return instanceId.split("@")[1];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;
        return Objects.equals(instanceId, instance.instanceId) && Objects.equals(service, instance.service);
    }
}
