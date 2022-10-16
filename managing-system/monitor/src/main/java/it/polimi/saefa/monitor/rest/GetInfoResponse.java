package it.polimi.saefa.monitor.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetInfoResponse {
    private int schedulingPeriod;
    private boolean isRoutineRunning;
}
