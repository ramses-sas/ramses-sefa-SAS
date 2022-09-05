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
public class Instance implements Serializable {
    private String address;
    private Service service;
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;

    private List<InstanceMetrics> metrics = new LinkedList<>();

    public Instance(String address, Service service) {
        this.address = address;
        this.service = service;
    }

    public Instance(String address, Service service, InstanceStatus currentStatus) {
        this.address = address;
        this.service = service;
        this.currentStatus = currentStatus;
    }

    public void addMetric(InstanceMetrics metric){
        metrics.add(metric);
    }

    public String getInstanceId(){
        return service.getName() + "@" + address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;
        return Objects.equals(address, instance.address) && Objects.equals(service, instance.service);
    }
}
