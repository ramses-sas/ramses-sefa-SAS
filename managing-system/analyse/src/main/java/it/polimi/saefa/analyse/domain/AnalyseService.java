package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.AdaptationParameter;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.HttpRequestMetrics;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.DateFormatter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@org.springframework.stereotype.Service
public class AnalyseService {
    //Sliding window con tot nuove e tot vecchie
    private Date lastAnalysisTimestamp = Date.from(Instant.MIN);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private int analysisWindow = 30;
    private int analysisWindowStep = 20; // 10 metriche vecchie, 20 metriche nuove
    @Autowired
    private KnowledgeClient knowledgeClient;
    public void startAnalysis() {

        //ci serve il set di servizi per ottenere la cofigurazione attuale
        List<Service> currentServices = knowledgeClient.getServices();
        for(Service service : currentServices) {
            for(Instance instance : service.getInstances()){
                List<InstanceMetrics> metrics = new LinkedList<>();
                int afterMetrics = analysisWindowStep;
                metrics.addAll(knowledgeClient.getLatestNMetricsBeforeDate(instance.getInstanceId(), dateFormatter.format(lastAnalysisTimestamp), analysisWindow - analysisWindowStep));
                if(metrics.size() != analysisWindow - analysisWindowStep){
                    afterMetrics = analysisWindow - metrics.size();
                }
                metrics.addAll(knowledgeClient.getLatestNMetricsAfterDate(instance.getInstanceId(), dateFormatter.format(lastAnalysisTimestamp), afterMetrics));

                if(metrics.size() != analysisWindow){
                    return; //TODO gestire
                }
                metrics.sort(Comparator.comparing(InstanceMetrics::getTimestamp));

                Map<String, Double> endpointAvgRespTime = new HashMap<>();
                Map<String, Double> endpointMaxRespTime = new HashMap<>();
                InstanceMetrics firstMetrics = metrics.get(0);
                InstanceMetrics lastMetrics = metrics.get(metrics.size() - 1);

                for(String endpoint : firstMetrics.getHttpMetrics().keySet()){
                    double durationDifference = lastMetrics.getHttpMetrics().get(endpoint).getTotalDuration() - firstMetrics.getHttpMetrics().get(endpoint).getTotalDuration();
                    double requestDifference = lastMetrics.getHttpMetrics().get(endpoint).getCount() - firstMetrics.getHttpMetrics().get(endpoint).getCount();
                    endpointAvgRespTime.put(endpoint, durationDifference/requestDifference);
                    endpointMaxRespTime.put(endpoint, lastMetrics.getHttpMetrics().get(endpoint).getMaxDuration());
                }
            }

        }
        lastAnalysisTimestamp = new Date();
        //creare lista di liste di servizi da mandare al pranner
    }

    private double computeAvailability(Service service) {
        return 0;
    }

    private double computeAverageResponseTime(Service service) {
        return 0;
    }

    private double computeMaxResponseTime(Service service) {
        return 0;
    }


}
