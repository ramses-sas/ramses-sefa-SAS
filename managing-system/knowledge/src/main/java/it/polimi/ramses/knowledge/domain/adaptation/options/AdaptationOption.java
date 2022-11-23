package it.polimi.ramses.knowledge.domain.adaptation.options;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.polimi.ramses.knowledge.domain.adaptation.specifications.QoSSpecification;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "DISCRIMINATOR", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddInstanceOption.class),
        @JsonSubTypes.Type(value = ShutdownInstanceOption.class),
        @JsonSubTypes.Type(value = ChangeLoadBalancerWeightsOption.class),
        @JsonSubTypes.Type(value = ChangeImplementationOption.class),
})
public abstract class AdaptationOption {
    @Id
    @GeneratedValue
    private long id;

    private String serviceId;
    private String serviceImplementationId;
    private String comment;
    private Class<? extends QoSSpecification> qosGoal;

    private boolean forced = false;

    // Timestamp of acceptance (it is NOT NULL ONLY IF the adaptation option has been accepted by the Plan)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public abstract String getDescription();

    public void applyTimestamp() {
        this.timestamp = new Date();
    }

    public AdaptationOption(String serviceId, String serviceImplementationId, String comment) {
        this.serviceId = serviceId;
        this.serviceImplementationId = serviceImplementationId;
        this.comment = comment;
    }

    @Override
    public String toString() {
        return getDescription();
    }

}
