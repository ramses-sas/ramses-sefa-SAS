package it.polimi.saefa.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
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
public class ChangeLoadBalancerWeightsOption extends AdaptationOption {
    @ElementCollection
    // <instanceId, newWeight>
    private Map<String, Double> newWeights;


    public ChangeLoadBalancerWeightsOption(String serviceId, String serviceImplementationId, Class<? extends AdaptationParamSpecification> goal, String comment) {
        super(serviceId, serviceImplementationId, comment);
        super.setAdaptationParametersGoal(goal);
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        String base = "Goal: " + getAdaptationParametersGoal().getSimpleName() + " - Change load balancer weights of service " + super.getServiceId();
        if (newWeights != null)
            base += "\nNew weights are: \n" + newWeights;
        return base + ".\n" + getComment();
    }
}
