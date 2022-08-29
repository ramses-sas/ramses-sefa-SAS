package it.polimi.saefa.knowledge.persistence;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public class HttpRequestMetrics {
    public String endpoint;
    public String httpMethod;
    public String outcome;
    public int status;
    public long count;
    public double totalDuration;

    public double getAverageDuration() {
        return totalDuration / count;
    }

    @Override
    public String toString() {
        return "HttpRequestMetrics{ " +
                httpMethod + " " + endpoint +
                ", outcome=" + outcome +
                ", status=" + status +
                ", requestsCount=" + count +
                ", totalDuration=" + totalDuration +
                '}';
    }
}



