package it.polimi.saefa.knowledge.domain.persistence;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
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
    private double currentValue;
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public QoSValueEntity(String serviceId, String serviceImplementationId, String instanceId, String qos, double value, boolean invalidatesThisAndPrevious, double currentValue, Date timestamp) {
        this.serviceId = serviceId;
        this.serviceImplementationId = serviceImplementationId;
        this.instanceId = instanceId;
        this.qos = qos;
        this.value = value;
        this.invalidatesThisAndPrevious = invalidatesThisAndPrevious;
        this.currentValue = currentValue;
        this.timestamp = timestamp;
    }
}
