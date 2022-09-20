package it.polimi.saefa.knowledge.persistence.domain.adaptation;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import lombok.Getter;
import java.util.List;

@Getter
public class AdaptationOption {

    private final Type type;
    private final String description;
    private final Service service;
    private final Instance instance;
    private final List<ServiceImplementation> serviceImplementationList;

    public enum Type {
        ADD_INSTANCES,
        REMOVE_INSTANCE,
        CHANGE_SERVICE_IMPLEMENTATION,
        CHANGE_LOADBALANCER_WEIGHTS,
        CHANGE_CIRCUITBREAKER_PARAMETERS
    }


    private AdaptationOption(Type type, String description, Service service, Instance instance, List<ServiceImplementation> serviceImplementationList) {
        this.type = type;
        this.description = description;
        this.service = service;
        this.instance = instance;
        this.serviceImplementationList = serviceImplementationList;
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
}
