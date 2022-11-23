package it.polimi.ramses.execute.externalInterfaces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartNewInstancesRequest {
    private String serviceImplementationName;
    private int numberOfInstances;
}
