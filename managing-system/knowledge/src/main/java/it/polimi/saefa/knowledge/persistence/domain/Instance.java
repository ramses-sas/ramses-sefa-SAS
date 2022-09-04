package it.polimi.saefa.knowledge.persistence.domain;

import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import it.polimi.saefa.knowledge.persistence.InstanceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Instance implements Serializable {
    @Id
    private String address;
    @Id
    private String serviceId;
    private InstanceStatus currentStatus = InstanceStatus.ACTIVE;

    @OneToMany(cascade = CascadeType.ALL)
    private List<InstanceMetrics> metrics;

    public Instance(String address, String serviceId) {
        this.address = address;
        this.serviceId = serviceId;
    }

    public Instance(String address, String serviceId, InstanceStatus currentStatus) {
        this.address = address;
        this.serviceId = serviceId;
        this.currentStatus = currentStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instance instance = (Instance) o;
        return Objects.equals(address, instance.address) && Objects.equals(serviceId, instance.serviceId);
    }
}
