package it.polimi.saefa.knowledge.rest.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBenchmarkRequest {
    private String serviceImplementationId;
    private Class<? extends it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification> qos;
    private Double newValue;
}
