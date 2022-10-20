package it.polimi.saefa.knowledge.rest;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class SetLoadBalancerWeightsRequest {
    // <serviceId, <instanceId, weight>>
    Map<String, Map<String, Double>> servicesLBWeights;
}
