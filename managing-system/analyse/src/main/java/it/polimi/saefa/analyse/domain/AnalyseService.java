package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Instance;
import it.polimi.saefa.knowledge.persistence.domain.architecture.InstanceStatus;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.metrics.HttpRequestMetrics;
import it.polimi.saefa.knowledge.persistence.domain.metrics.InstanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Slf4j
@org.springframework.stereotype.Service
public class AnalyseService {
    private Date lastAnalysisTimestamp = Date.from(Instant.ofEpochMilli(0));
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    //Sliding window of size `analysisWindowSize`, containing `analysisWindowStep` new metrics
    @Value("${analysisWindowSize}")
    private int analysisWindowSize;
    @Value("${FAILURE_RATE_THRESHOLD}")
    private double failureRateThreshold;
    @Value("${UNREACHABLE_RATE_THRESHOLD}")
    private double unreachableRateThreshold;

    @Autowired
    private KnowledgeClient knowledgeClient;

    public void startAnalysis() {
        log.warn("Starting analysis");
        List<Service> currentArchitecture = knowledgeClient.getServices();
        List<ServiceStats> currentArchitectureStats = new LinkedList<>();
        for (Service service : currentArchitecture) {
            ServiceStats serviceStats = new ServiceStats(service);
            // Analyze all the instances
            for (Instance instance : service.getInstances()) {
                if(instance.getCurrentStatus() == InstanceStatus.SHUTDOWN) {
                    continue;
                }
                List<InstanceMetrics> metrics = new LinkedList<>();
                /*
                int afterMetrics = analysisWindowStep;
                metrics.addAll(knowledgeClient.getLatestNMetricsBeforeDate(instance.getInstanceId(), dateFormatter.format(lastAnalysisTimestamp), analysisWindowSize - analysisWindowStep));
                if (metrics.size() != analysisWindowSize - analysisWindowStep) {
                    afterMetrics = analysisWindowSize - metrics.size();
                }
                metrics.addAll(knowledgeClient.getLatestNMetricsAfterDate(instance.getInstanceId(), dateFormatter.format(lastAnalysisTimestamp), afterMetrics));
                 */
                metrics.addAll(knowledgeClient.getLatestNMetricsOfCurrentInstance(instance.getInstanceId(), analysisWindowSize));
                // Not enough data to perform analysis
                if (metrics.size() != analysisWindowSize) {
                    return; //TODO gestire, direi che semplicemente viene chiamato il planner con lista di istruzioni vuota
                }
                metrics.sort(Comparator.comparing(InstanceMetrics::getTimestamp)); //todo da togliere, forse


                //per ciascuna metrica, devo prendere tutti gli endpoint e contare le richieste fallite e le richieste andate a buon fine

                /*se c'è una metrica unreachable abbiamo 4 casi, riassumibili con la seguente regex: UNREACHABLE+ FAILED* ACTIVE*
                Se c'è una failed di mezzo, la consideriamo come il caso di sotto.
                Se finisce con active, tutto a posto a meno di anomalie nell'unreachable rate


                //se c'è una failed ma non una unreachable, abbiamo 2 casi, riassumibili con la seguente regex: FAILED+ ACTIVE*.
                Caso solo failed: considerare l'istanza come spenta, ignorarla nel calcolo degli adaptation parameters.
                Caso last metric active: tutto ok, a meno di conti sul tasso di faild e unreachable

                Calcolare unreachable rate e failure rate.
                Se superiori a delle soglie, considerarle faulty e quindi da spegnere e non cosniderare nel calcolo dell'avilability


                 */
                InstanceMetrics firstMetrics = metrics.get(0);
                InstanceMetrics lastMetrics = metrics.get(metrics.size() - 1);

                failureRateThreshold = Math.min(failureRateThreshold, 1.0);
                unreachableRateThreshold = Math.min(unreachableRateThreshold, 1.0);

                double failureRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isFailed() ? 1:0), Double::sum) / metrics.size();
                double unreachableRate = metrics.stream().reduce(0.0, (acc, m) -> acc + (m.isUnreachable() ? 1:0), Double::sum) / metrics.size();

                if(lastMetrics.isFailed() || unreachableRate >= unreachableRateThreshold || failureRate >= failureRateThreshold) { //in ordine di probabilità
                    //TODO Prova a spegnere l'istanza forzatamente, non aspettarti una risposta.
                    // !!!L'istanza va marcata come shutdown!!!.
                    // Discorso dipendenze: provare ad approfondire
                    continue;

                    /*
                    Se l'utlima metrica è failed, allora l'istanza è crashata. Va marcata come istanza spenta per non
                    confonderla con una potenziale nuova istanza con stesso identificatore.
                    Inoltre, per evitare comportamenti oscillatori, scegliamo di terminare istanze poco reliable che
                    sono spesso unreachable o failed.
                     */
                }

                InstanceMetrics firstActiveMetrics = metrics.stream().filter(InstanceMetrics::isActive).min(Comparator.comparing(InstanceMetrics::getTimestamp)).orElse(null);
                InstanceMetrics lastActiveMetrics = metrics.stream().filter(InstanceMetrics::isActive).max(Comparator.comparing(InstanceMetrics::getTimestamp)).orElse(null);

                if(firstActiveMetrics == lastActiveMetrics){ //non ci sono abbastanza metriche per questa istanza
                   continue;
                }

                if(firstActiveMetrics != firstMetrics) {
                    log.error("first metrics mismatch"); //TODO rimuovere
                }

                // <endpoint, value>
                Map<String, Double> endpointAvgRespTime = new HashMap<>();
                Map<String, Double> endpointMaxRespTime = new HashMap<>();

                for (String endpoint : firstActiveMetrics.getHttpMetrics().keySet()) {
                    double durationDifference = lastActiveMetrics.getHttpMetrics().get(endpoint).getTotalDuration() - firstActiveMetrics.getHttpMetrics().get(endpoint).getTotalDuration();
                    double requestDifference = lastActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount() - firstActiveMetrics.getHttpMetrics().get(endpoint).getTotalCount();
                    if (requestDifference != 0)
                        endpointAvgRespTime.put(endpoint, durationDifference / requestDifference);
                    endpointMaxRespTime.put(endpoint, lastActiveMetrics.getHttpMetrics().get(endpoint).getMaxDuration());
                }

                // Single instance statistics (i.e., values that the analysis use to compute adaptation options)
                InstanceStats instanceStats = new InstanceStats(instance, computeInstanceAvgResponseTime(endpointAvgRespTime),
                        computeInstanceMaxResponseTime(endpointMaxRespTime), computeInstanceAvailability(firstActiveMetrics, lastActiveMetrics));
                serviceStats.addInstanceStats(instanceStats);
            }
            serviceStats.updateStats();

            // Inutile?
            currentArchitectureStats.add(serviceStats);
        }
        log.warn("Ending analysis");
        lastAnalysisTimestamp = new Date();
        //creare set di possibili istruzioni da mandare al Plan
    }

    private void handleMaxResponseTime(ServiceStats serviceStats) {
        //TODO
    }

    private Double computeInstanceAvailability(InstanceMetrics firstMetrics, InstanceMetrics lastMetrics) {
        double successfulRequests = 0;
        double failedRequests = 0;

        for(HttpRequestMetrics httpMetric : lastMetrics.getHttpMetrics().values()){
            for(HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()){
                if(outcomeMetric.getOutcome().equalsIgnoreCase("success")){ //todo controllare gli endpoint che ritornano 100 o 300
                    successfulRequests += outcomeMetric.getCount();
                } else {
                    failedRequests += outcomeMetric.getCount();
                }
            }
        }

        for(HttpRequestMetrics httpMetric : firstMetrics.getHttpMetrics().values()){
            for(HttpRequestMetrics.OutcomeMetrics outcomeMetric : httpMetric.getOutcomeMetrics().values()){
                if(outcomeMetric.getOutcome().equalsIgnoreCase("success")){ //todo vedi sopra
                    successfulRequests -= outcomeMetric.getCount();
                } else {
                    failedRequests -= outcomeMetric.getCount();
                }
            }
        }

        return successfulRequests / (successfulRequests + failedRequests);
    }

    private Double computeInstanceMaxResponseTime(Map<String, Double> endpointMaxRespTime) {
        return endpointMaxRespTime.values().stream().max(Double::compareTo).orElse(null);
    }

    private Double computeInstanceAvgResponseTime(Map<String, Double> endpointAvgRespTime) {
        return endpointAvgRespTime.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }


}
