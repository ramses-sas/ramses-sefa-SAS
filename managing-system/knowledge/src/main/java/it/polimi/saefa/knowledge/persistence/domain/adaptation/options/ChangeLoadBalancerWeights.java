package it.polimi.saefa.knowledge.persistence.domain.adaptation.options;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@Entity
@DiscriminatorValue("CHANGE_LOAD_BALANCER_WEIGHTS")
public class ChangeLoadBalancerWeights extends AdaptationOption{
    @ElementCollection
    private Map<String, Double> newWeights = new HashMap<>();

    public ChangeLoadBalancerWeights(String serviceId, String serviceImplementationId) {
        super(serviceId, serviceImplementationId);
    }

    @Override
    public String getDescription() {
        return "Change load balancer weights of service " + super.getServiceId();
    }
}
