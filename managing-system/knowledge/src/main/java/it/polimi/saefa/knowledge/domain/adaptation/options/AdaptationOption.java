package it.polimi.saefa.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "DISCRIMINATOR", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddInstances.class),
        @JsonSubTypes.Type(value = RemoveInstance.class),
        @JsonSubTypes.Type(value = ChangeLoadBalancerWeights.class),
})
public abstract class AdaptationOption {
    @Id
    @GeneratedValue
    private long id;

    private String serviceId;
    private String serviceImplementationId;

    private String comment;
    private boolean forced = false;

    // Timestamp of acceptance (it is NOT NULL ONLY IF the adaptation option has been accepted by the Plan)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public abstract String getDescription();

    public void applyTimestamp() {
        this.timestamp = new Date();
    }

    public AdaptationOption(String serviceId, String serviceImplementationId, String comment) {
        this.serviceId = serviceId;
        this.serviceImplementationId = serviceImplementationId;
        this.comment = comment;
    }

    /*
    public Double getRequiredValue(Class<? extends AdaptationParamSpecification> adaptationParamClass) {
        return requiredValueMap.get(adaptationParamClass);
    }

    public void addRequiredValue(Class<? extends AdaptationParamSpecification> adaptationParamClass, Double value) {
        requiredValueMap.put(adaptationParamClass, value);
    }

    public static AdaptationOption addNewInstances(Service service) {
        return new AdaptationOption(Type.ADD_INSTANCES, "Add new instances of service " + service.getServiceId(), service, null, null);
    }

    public static AdaptationOption removeInstance(Service service, Instance instanceId) {
        return new AdaptationOption(Type.REMOVE_INSTANCE, "Remove instance " + instanceId + " of service " + service.getServiceId(), service, instanceId, null);
    }

    public static AdaptationOption changeImplementation(Service service, List<ServiceImplementation> serviceImplementationList) {
        return new AdaptationOption(Type.CHANGE_SERVICE_IMPLEMENTATION, "Change implementation of service " + service.getServiceId() + ". Possible implementations: " + serviceImplementationList, service, null, serviceImplementationList);
    }

    public static AdaptationOption changeLoadBalancerWeights(Service service) {
        return new AdaptationOption(Type.CHANGE_LOADBALANCER_WEIGHTS, "Change load balancer weights of service " + service.getServiceId(), service, null, null);
    }

    public static AdaptationOption changeCircuitBreakerParameters(Service service) {
        return new AdaptationOption(Type.CHANGE_CIRCUITBREAKER_PARAMETERS, "Change circuit breaker parameters of service " + service.getServiceId(), service, null, null);
    }

    @Override
    public String toString() {
        return "AdaptationOptions{" +
                "type='" + type + '\'' +
                "description='" + description + '\'' +
                '}';
    }

     */
}
