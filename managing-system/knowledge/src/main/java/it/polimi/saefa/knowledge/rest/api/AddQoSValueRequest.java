package it.polimi.saefa.knowledge.rest.api;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Getter;

import java.util.Date;

@Getter
public class AddQoSValueRequest {
    String serviceId;
    String instanceId;
    Class<? extends QoSSpecification> qoSClass;
    Double value;
    Date date;

    private AddQoSValueRequest() {
    }

    public static AddQoSValueRequest createServiceRequest(String serviceId, Class<? extends QoSSpecification> qoSClass, Double value, Date date) {
        AddQoSValueRequest request = new AddQoSValueRequest();
        request.serviceId = serviceId;
        request.instanceId = null;
        request.qoSClass = qoSClass;
        request.value = value;
        request.date = date;
        return request;
    }

    public static AddQoSValueRequest createInstanceRequest(String serviceId, String instanceId, Class<? extends QoSSpecification> qoSClass, Double value, Date date) {
        AddQoSValueRequest request = new AddQoSValueRequest();
        request.serviceId = serviceId;
        request.instanceId = instanceId;
        request.qoSClass = qoSClass;
        request.value = value;
        request.date = date;
        return request;
    }
}
