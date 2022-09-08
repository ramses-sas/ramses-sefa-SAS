package it.polimi.saefa.knowledge.rest;

import it.polimi.saefa.knowledge.persistence.domain.adaptation.AdaptationParameter;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOverview {
    private String serviceId;
    private String currentImplementation; //name of the current implementation of the service

    private ServiceConfiguration configuration;
    // <instanceId, Instance>
    private Map<String, InstanceOverview> instances = new HashMap<>();
    // <instanceId, ServiceImplementation>
    private Map<String, ServiceImplementation> possibleImplementations = new HashMap<>();

    private List<AdaptationParameter> adaptationParameters = new LinkedList<>();

    @Data
    static class InstanceOverview {
        private String instanceId;
        private String status;
        private InstanceMetrics latestMetrics;
    }
}
