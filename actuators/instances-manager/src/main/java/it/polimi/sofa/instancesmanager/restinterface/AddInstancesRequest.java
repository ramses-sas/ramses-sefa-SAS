package it.polimi.sofa.instancesmanager.restinterface;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddInstancesRequest {
    private String serviceImplementationName;
    private int numberOfInstances;
}
