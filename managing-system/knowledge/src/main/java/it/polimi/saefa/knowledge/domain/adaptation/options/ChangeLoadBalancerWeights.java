package it.polimi.saefa.knowledge.domain.adaptation.options;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Map;

@Getter
@Entity
@NoArgsConstructor
@DiscriminatorValue("CHANGE_LOAD_BALANCER_WEIGHTS")
public class ChangeLoadBalancerWeights extends AdaptationOption {
    // <instanceId, newWeight>
    @ElementCollection
    @Setter
    private Map<String, Double> newWeights;
    private Double serviceAverageAvailability;
    //private Double serviceAverageResponseTime;


    public ChangeLoadBalancerWeights(String serviceId, String serviceImplementationId, Double serviceAverageAvailability, String comment) {
        super(serviceId, serviceImplementationId, comment);
        this.serviceAverageAvailability = serviceAverageAvailability;
    }

    @Override
    public String getDescription() {
        String base = "Change load balancer weights of service " + super.getServiceId();
        if (newWeights != null)
            base += "\nNew weights are: \n" + newWeights;
        return base + ". " + getComment();
    }
}
