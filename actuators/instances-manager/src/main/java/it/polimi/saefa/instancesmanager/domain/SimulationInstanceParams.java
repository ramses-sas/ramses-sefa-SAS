package it.polimi.saefa.instancesmanager.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimulationInstanceParams {
    private Double exceptionProbability;
    private Double sleepDuration;
    private Double sleepVariance;
}
