package it.polimi.saefa.knowledge.persistence.domain;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;



@Entity
@Getter
@Setter
@NoArgsConstructor
public class HttpRequestMetrics {

    @Id
    @GeneratedValue
    private Long id;

    private String endpoint;
    private String httpMethod;
    private String outcome;
    private int status;
    private long count;
    private double totalDuration;

    public double getAverageDuration() {
        return totalDuration / count;
    }

    public HttpRequestMetrics(String endpoint, String httpMethod, String outcome, int status, long count, double totalDuration) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.outcome = outcome;
        this.status = status;
        this.count = count;
        this.totalDuration = totalDuration;
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



