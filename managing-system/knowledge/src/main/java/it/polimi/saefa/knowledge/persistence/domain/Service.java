package it.polimi.saefa.knowledge.persistence.domain;

import it.polimi.saefa.knowledge.persistence.InstanceStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Service {
    @Id
    private String name;
    @OneToOne
    private ServiceConfiguration configuration;
    @OneToMany(cascade = CascadeType.ALL)
    private Set<Instance> instances;

    public boolean isReachable(){
        for(Instance instance : instances){
            if(instance.getCurrentStatus() == InstanceStatus.ACTIVE){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return name.equals(service.name);
    }
}
