package it.polimi.saefa.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DiscriminatorValue("CHANGE_LOAD_BALANCER_WEIGHTS")
public class ChangeLoadBalancerWeightsOption extends AdaptationOption {
    @ElementCollection
    // <instanceId, newWeight>
    private Map<String, Double> newWeights; //Contains only the weights of the instances that will remain active
    @ElementCollection // TODO TODO TODO VA POPOLATO!!!!!!!
    private List<String> instancesToShutdownIds = new LinkedList<>();


    public ChangeLoadBalancerWeightsOption(String serviceId, String serviceImplementationId, Class<? extends QoSSpecification> goal, String comment) {
        super(serviceId, serviceImplementationId, comment);
        super.setQosGoal(goal);
    }

    @JsonIgnore
    @Override
    public String getDescription() {
        String base = "Goal: " + getQosGoal().getSimpleName() + " - Change LBW. Service: " + super.getServiceId();
        if (newWeights != null)
            base += "\n\t\t\t\t\tNew weights are: \n\t\t\t\t\t" + newWeights;
        return base + ".\n\t\t\t\t\t" + getComment();
    }
}
