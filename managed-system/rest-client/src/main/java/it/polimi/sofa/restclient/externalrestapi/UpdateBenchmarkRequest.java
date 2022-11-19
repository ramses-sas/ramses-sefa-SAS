package it.polimi.sofa.restclient.externalrestapi;

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
