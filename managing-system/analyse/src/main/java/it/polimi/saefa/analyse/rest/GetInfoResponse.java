package it.polimi.saefa.analyse.rest;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetInfoResponse {
    //Number of new metrics to analyse for each instance of each service
    private int metricsWindowSize;
    //Number of analysis iterations to do before choosing the best adaptation options
    private int analysisWindowSize;
    private double failureRateThreshold;
    private double unreachableRateThreshold;
    private double parametersSatisfactionRate;
}
