package it.polimi.saefa.knowledge.rest;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.Getter;

@Getter
public class AddAdaptationParameterValueRequest {
    String serviceId;
    String instanceId;
    Class<? extends AdaptationParamSpecification> adaptationParameterClass;
    Double value;

    private AddAdaptationParameterValueRequest() {
    }

    public static AddAdaptationParameterValueRequest createServiceRequest(String serviceId, Class<? extends AdaptationParamSpecification> adaptationParameterClass, Double value) {
        AddAdaptationParameterValueRequest request = new AddAdaptationParameterValueRequest();
        request.serviceId = serviceId;
        request.instanceId = null;
        request.adaptationParameterClass = adaptationParameterClass;
        request.value = value;
        return request;
    }

    public static AddAdaptationParameterValueRequest createInstanceRequest(String serviceId, String instanceId, Class<? extends AdaptationParamSpecification> adaptationParameterClass, Double value) {
        AddAdaptationParameterValueRequest request = new AddAdaptationParameterValueRequest();
        request.serviceId = serviceId;
        request.instanceId = instanceId;
        request.adaptationParameterClass = adaptationParameterClass;
        request.value = value;
        return request;
    }
}
