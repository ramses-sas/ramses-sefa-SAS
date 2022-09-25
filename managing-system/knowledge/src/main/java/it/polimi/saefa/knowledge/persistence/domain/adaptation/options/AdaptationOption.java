package it.polimi.saefa.knowledge.persistence.domain.adaptation.options;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.values.AdaptationParameter;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class AdaptationOption {

    private final Service service;
    private final ServiceImplementation serviceImplementation;
    @Setter
    private Map<Class<? extends AdaptationParamSpecification>, Double> requiredValueMap = new HashMap<>();
    /*
    private final Instance instance;
    private final List<ServiceImplementation> serviceImplementationList;
    private Double improvement; //TODO se non si pensa a una soluzione diversa, rendere questa una classe astratta che va implementata dalle diverse opzioni di adattamento
     */
    /*public enum Type {
        ADD_INSTANCES,
        REMOVE_INSTANCE,
        CHANGE_SERVICE_IMPLEMENTATION,
        CHANGE_LOADBALANCER_WEIGHTS,
        CHANGE_CIRCUITBREAKER_PARAMETERS
    }

     */

    public abstract String getDescription();


    public AdaptationOption(Service service) {
        this.service = service;
        this.serviceImplementation = service.getCurrentImplementationObject();
    }

    public AdaptationOption(Service service, ServiceImplementation serviceImplementation) {
        this.service = service;
        this.serviceImplementation = serviceImplementation;
    }

    public Double getRequiredValue(Class<? extends AdaptationParamSpecification> adaptationParamClass) {
        return requiredValueMap.get(adaptationParamClass);
    }

    public void addRequiredValue(Class<? extends AdaptationParamSpecification> adaptationParamClass, Double value) {
        requiredValueMap.put(adaptationParamClass, value);
    }


    /*
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
