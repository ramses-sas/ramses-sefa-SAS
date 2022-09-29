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
@Entity
@NoArgsConstructor
@DiscriminatorValue("CHANGE_LOAD_BALANCER_WEIGHTS")
public class ChangeLoadBalancerWeights extends AdaptationOption {
    // <instanceId, newWeight>
    @ElementCollection
    private Map<String, Double> newWeights;

    public ChangeLoadBalancerWeights(String serviceId, String serviceImplementationId) {
        super(serviceId, serviceImplementationId);
    }

    @Override
    public String getDescription() {
        String base = "Change load balancer weights of service " + super.getServiceId();
        if (newWeights != null)
            base += "\nNew weights are: \n" + newWeights;
        return base;
    }
}
