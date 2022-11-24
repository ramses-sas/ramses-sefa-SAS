package it.polimi.ramses.execute.externalInterfaces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeLBWeightsRequest {
    private String serviceId;
    // <instanceId, weight>
    private Map<String, Double> newWeights;
    // <instanceId>
    private List<String> instancesToRemoveWeightOf;
}
