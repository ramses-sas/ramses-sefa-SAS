package it.polimi.saefa.knowledge.domain.adaptation.options;

import it.polimi.saefa.knowledge.domain.adaptation.values.AdaptationParameter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DiscriminatorValue("CHANGE_LOAD_BALANCER_WEIGHTS")
public class ChangeLoadBalancerWeights extends AdaptationOption {
    @ElementCollection
    // <instanceId, newWeight>
    private Map<String, Double> newWeights;


    public ChangeLoadBalancerWeights(String serviceId, String serviceImplementationId, String comment) {
        super(serviceId, serviceImplementationId, comment);
        //this.serviceAverageAvailability = serviceAverageAvailability;
    }

    @Override
    public String getDescription() {
        String base = "Change load balancer weights of service " + super.getServiceId();
        if (newWeights != null)
            base += "\nNew weights are: \n" + newWeights;
        return base + ". " + getComment();
    }
}
