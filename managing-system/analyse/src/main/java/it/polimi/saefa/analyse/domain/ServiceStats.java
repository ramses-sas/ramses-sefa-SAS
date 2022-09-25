package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ServiceStats {
    private Service service;
    private List<InstanceStats> instancesStats = new ArrayList<>();
    private Double averageResponseTime;
    private Double maxResponseTime;
    private Double availability;

    public ServiceStats(Service service) {
        this.service = service;
    }

    public String getServiceId() {
        return service.getServiceId();
    }

    public void addInstanceStats(InstanceStats instanceStats) {
        instancesStats.add(instanceStats);
    }

    public void updateStats() {
        updateAverageResponseTime();
        updateMaxResponseTime();
        updateAvailability();
    }

    public void updateAverageResponseTime() {
        if (instancesStats.isEmpty())
            averageResponseTime = null;
        averageResponseTime = instancesStats.stream().mapToDouble(InstanceStats::getAverageResponseTime).average().orElseThrow();
    }

    public void updateMaxResponseTime() {
        if (instancesStats.isEmpty())
            maxResponseTime = null;
        maxResponseTime = instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow();
    }

    public void updateAvailability() {
        if (instancesStats.isEmpty())
            availability = null;
        availability = 1-instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1-val));
    }
}
