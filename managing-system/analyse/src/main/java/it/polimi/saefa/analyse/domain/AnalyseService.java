package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@org.springframework.stereotype.Service
public class AnalyseService {
    private Date lastAnalysisTimestamp = Date.from(Instant.MIN);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    //Sliding window of size `analysisWindowSize`, containing `analysisWindowStep` new metrics
    private final int analysisWindowSize = 30;
    private final int analysisWindowStep = 20; // 10 metriche vecchie, 20 metriche nuove

    @Autowired
    private KnowledgeClient knowledgeClient;

    public void startAnalysis() {
        List<Service> currentArchitecture = knowledgeClient.getServices();
        List<ServiceStats> currentArchitectureStats = new LinkedList<>();
        for (Service service : currentArchitecture) {
            ServiceStats serviceStats = new ServiceStats(service);
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                List<InstanceMetrics> metrics = new LinkedList<>();
                int afterMetrics = analysisWindowStep;
                metrics.addAll(knowledgeClient.getLatestNMetricsBeforeDate(instance.getInstanceId(), dateFormatter.format(lastAnalysisTimestamp), analysisWindowSize - analysisWindowStep));
                if (metrics.size() != analysisWindowSize - analysisWindowStep) {
                    afterMetrics = analysisWindowSize - metrics.size();
                }
                metrics.addAll(knowledgeClient.getLatestNMetricsAfterDate(instance.getInstanceId(), dateFormatter.format(lastAnalysisTimestamp), afterMetrics));

                // Not enough data to perform analysis
                if (metrics.size() != analysisWindowSize) {
                    return; //TODO gestire
                }
                metrics.sort(Comparator.comparing(InstanceMetrics::getTimestamp));
                InstanceMetrics firstMetrics = metrics.get(0);
                InstanceMetrics lastMetrics = metrics.get(metrics.size() - 1);

                // <endpoint, value>
                Map<String, Double> endpointAvgRespTime = new HashMap<>();
                // <endpoint, value>
                Map<String, Double> endpointMaxRespTime = new HashMap<>();

                for (String endpoint : firstMetrics.getHttpMetrics().keySet()) {
                    double durationDifference = lastMetrics.getHttpMetrics().get(endpoint).getTotalDuration() - firstMetrics.getHttpMetrics().get(endpoint).getTotalDuration();
                    double requestDifference = lastMetrics.getHttpMetrics().get(endpoint).getCount() - firstMetrics.getHttpMetrics().get(endpoint).getCount();
                    if (requestDifference != 0)
                        endpointAvgRespTime.put(endpoint, durationDifference / requestDifference);
                    endpointMaxRespTime.put(endpoint, lastMetrics.getHttpMetrics().get(endpoint).getMaxDuration());
                }

                // Single instance statistics (i.e., values that the analysis use to compute adaptation options)
                InstanceStats instanceStats = new InstanceStats(instance, computeAvgResponseTime(endpointAvgRespTime),
                        computeMaxResponseTime(endpointMaxRespTime), computeAvailability(metrics));
                serviceStats.addInstanceStats(instanceStats);
            }
            serviceStats.updateStats();
            currentArchitectureStats.add(serviceStats);
        }
        lastAnalysisTimestamp = new Date();
        //creare set di possibili architetture da mandare al Plan
    }

    private void handleMaxResponseTime(ServiceStats serviceStats) {
        //TODO
    }

    private Double computeAvailability(List<InstanceMetrics> metrics) {
        return null;
    }

    private Double computeMaxResponseTime(Map<String, Double> endpointMaxRespTime) {
        return endpointMaxRespTime.values().stream().max(Double::compareTo).orElse(null);
    }

    private Double computeAvgResponseTime(Map<String, Double> endpointAvgRespTime) {
        return endpointAvgRespTime.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }


}
