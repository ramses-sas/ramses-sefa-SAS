package it.polimi.saefa.knowledge.domain.adaptation.options;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Map;

@Getter
@NoArgsConstructor
@Entity
@DiscriminatorValue("CHANGE_LOAD_BALANCER_WEIGHTS")
public class ChangeLoadBalancerWeights extends AdaptationOption{
    @ElementCollection
    @Setter
    private Map<String, Double> newWeights;
    private Double serviceAverageAvailability;
    //private Double serviceAverageResponseTime;


    public ChangeLoadBalancerWeights(String serviceId, String serviceImplementationId, Double serviceAverageAvailability) {
        super(serviceId, serviceImplementationId);
        this.serviceAverageAvailability = serviceAverageAvailability;
    }

    @Override
    public String getDescription() {
        return "Change load balancer weights of service " + super.getServiceId();
    }
}
