package it.polimi.sefa.instancesmanager.restinterface;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddInstancesTESTRequest {
    private String serviceImplementationName;
    private int numberOfInstances;
    private double exceptionRate;
    private double sleepDuration;
    private double sleepVariance;
}
