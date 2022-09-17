package it.polimi.saefa.knowledge.persistence.domain.metrics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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

    @ElementCollection
    //Map<OutcomeStatus, OutcomeMetrics>
    private Map<String, OutcomeMetrics> outcomeMetrics = new HashMap<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OutcomeMetrics{
        private String outcome;
        private int status;
        private long count;
        private double totalDuration;
        private double maxDuration;

        @JsonIgnore
        @Transient
        public double getAverageDuration() {
            if(count== 0)
                return 0;
            return totalDuration / count;
        }

    }

    @JsonIgnore
    public double getAverageDurationByOutcome(String outcome) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.get(outcome);
        if(outcomeMetric == null)
            return -1;
        return outcomeMetric.getAverageDuration();
    }

    @JsonIgnore
    public double getTotalDurationByOutcome(String outcome) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.get(outcome);
        if(outcomeMetric == null)
            return -1;
        return outcomeMetric.getTotalDuration();
    }

    @JsonIgnore
    public long getCountByOutcome(String outcome) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.get(outcome);
        if(outcomeMetric == null)
            return -1;
        return outcomeMetric.getCount();
    }

    @JsonIgnore
    public double getMaxDurationByOutcome(String outcome) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.get(outcome);
        if(outcomeMetric == null)
            return -1;
        return outcomeMetric.getMaxDuration();
    }

    @JsonIgnore
    public int getStatusByOutcome(String outcome) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.get(outcome);
        if(outcomeMetric == null)
            return -1;
        return outcomeMetric.getStatus();
    }

    @JsonIgnore
    public Set<String> getOutcomes() {
        return outcomeMetrics.keySet();
    }

    public void addOrSetOutcomeMetricsDetails(String outcome, int status, long count, double totalDuration) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.getOrDefault(outcome, new OutcomeMetrics());
        outcomeMetric.setStatus(status);
        outcomeMetric.setCount(count);
        outcomeMetric.setTotalDuration(totalDuration);
        outcomeMetrics.put(outcome, outcomeMetric);
    }

    public void addOrSetOutcomeMetricsMaxDuration(String outcome, double maxDuration) {
        OutcomeMetrics outcomeMetric = outcomeMetrics.getOrDefault(outcome, new OutcomeMetrics());
        outcomeMetric.setMaxDuration(maxDuration);
        outcomeMetrics.put(outcome, outcomeMetric);
    }




    public HttpRequestMetrics(String endpoint, String httpMethod) {
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
    }

    @Override
    public String toString() {
        return "HttpRequestMetrics{ " +
                httpMethod + " " + endpoint +
                '}';
    }
}



