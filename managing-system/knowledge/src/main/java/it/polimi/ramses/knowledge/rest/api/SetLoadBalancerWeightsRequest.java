package it.polimi.ramses.knowledge.rest.api;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class SetLoadBalancerWeightsRequest {
    // <serviceId, <instanceId, weight>>
    Map<String, Map<String, Double>> servicesLBWeights;
}
