package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
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

    private Double averageAvailability;
    private Double averageMaxResponseTime; //TODO forse non serve, capire effettivamente se serve

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
        updateEmptyStats();
        updateAverageResponseTime();
        updateMaxResponseTime();
        updateAvailability();
    }

    public void updateAverageResponseTime() {
        if (instancesStats.isEmpty())
            averageResponseTime = null;
        if (averageResponseTime == null)
            averageResponseTime = instancesStats.stream().mapToDouble(InstanceStats::getAverageResponseTime).average().orElseThrow();
    }

    public void updateMaxResponseTime() {
        if (instancesStats.isEmpty())
            maxResponseTime = null;
        else
            maxResponseTime = instancesStats.stream().mapToDouble(InstanceStats::getMaxResponseTime).max().orElseThrow();
    }

    public void updateAvailability() {
        if (instancesStats.isEmpty())
            availability = null;
        else
            availability = 1-instancesStats.stream().mapToDouble(InstanceStats::getAvailability).reduce(1.0, (accumulator, val) -> accumulator * (1-val));
    }

    public void updateEmptyStats(){
        if (instancesStats.isEmpty())
            return;
        List<InstanceStats> emptyStats = new LinkedList<>();
        double availabilityAccumulator = 0;
        double maxResponseTimeAccumulator = 0;
        double averageResponseTimeAccumulator = 0;
        for (InstanceStats instanceStats : instancesStats) {
            if (instanceStats.isDataUnavailable()) {
                emptyStats.add(instanceStats);
            } else {
                availabilityAccumulator += instanceStats.getAvailability();
                maxResponseTimeAccumulator += instanceStats.getMaxResponseTime();
                averageResponseTimeAccumulator += instanceStats.getAverageResponseTime();
            }
        }

        averageAvailability = availabilityAccumulator / (instancesStats.size() - emptyStats.size());
        averageMaxResponseTime = maxResponseTimeAccumulator / (instancesStats.size() - emptyStats.size());
        averageResponseTime = averageResponseTimeAccumulator / (instancesStats.size() - emptyStats.size());

        for (InstanceStats instanceStats : emptyStats) {
            instanceStats.setAvailability(averageAvailability);
            instanceStats.setMaxResponseTime(averageMaxResponseTime);
            instanceStats.setAverageResponseTime(averageResponseTime);
        }
    }
}
