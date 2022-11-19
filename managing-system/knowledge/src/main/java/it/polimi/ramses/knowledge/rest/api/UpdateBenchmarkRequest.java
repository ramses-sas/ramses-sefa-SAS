package it.polimi.ramses.knowledge.rest.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBenchmarkRequest {
    private String serviceImplementationId;
    private String qos;
    private Double newValue;
}
