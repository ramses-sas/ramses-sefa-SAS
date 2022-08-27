package it.polimi.saefa.monitor;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class HttpRequestMetrics {
    public String path;
    public String httpMethod;
    public String outcome;
    public int status;
    public long count;
    public double totalDuration;

    public double getAverageDuration() {
        return totalDuration / count;
    }
}



