package it.polimi.saefa.instancesmanager.restinterface;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddInstancesRequest {
    private String serviceId;
    private String serviceImplementationName;
    private int numberOfInstances;
}
