package it.polimi.saefa.knowledge.domain.persistence;

import it.polimi.saefa.knowledge.domain.adaptation.values.QoSHistory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class QoSValueEntity {
    @Id
    @GeneratedValue
    private long id;
    private String serviceId;
    private String serviceImplementationId;
    private String instanceId; //if null, the value is for the service
    private String qos;
    private double value;
    private boolean invalidatesThisAndPrevious;
    private Double currentValue;
    private double threshold;
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public QoSValueEntity(String serviceId, String serviceImplementationId, String instanceId, String qos, double threshold, Double currentValue, double value, boolean invalidatesThisAndPrevious, Date timestamp) {
        this.serviceId = serviceId;
        this.serviceImplementationId = serviceImplementationId;
        this.instanceId = instanceId;
        this.qos = qos;
        this.value = value;
        this.invalidatesThisAndPrevious = invalidatesThisAndPrevious;
        this.currentValue = currentValue;
        this.timestamp = timestamp;
    }

    public QoSValueEntity(String serviceId, String serviceImplementationId, String instanceId, String qos, double threshold, QoSHistory.Value currentValue, QoSHistory.Value value) {
        this(serviceId, serviceImplementationId, instanceId, qos, threshold, (currentValue == null ? null : currentValue.getDoubleValue()), value.getDoubleValue(), value.invalidatesThisAndPreviousValues(), value.getTimestamp());
    }
}
