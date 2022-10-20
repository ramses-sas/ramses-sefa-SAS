package it.polimi.saefa.knowledge.rest;

import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import it.polimi.saefa.knowledge.domain.adaptation.values.QoSHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateServiceQosCollectionRequest {
    private String serviceId;
    private Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesValues;
    private Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceValues;
    private Map<String, Map<Class<? extends QoSSpecification>, QoSHistory.Value>> newInstancesCurrentValues;
    private Map<Class<? extends QoSSpecification>, QoSHistory.Value> newServiceCurrentValues;
}
