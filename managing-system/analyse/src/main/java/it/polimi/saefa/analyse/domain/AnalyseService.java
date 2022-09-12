package it.polimi.saefa.analyse.domain;

import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

@org.springframework.stereotype.Service
public class AnalyseService {
    //Sliding window con tot nuove e tot vecchie
    private Date lastAnalysisTimestamp;
    private int analysisWindow = 20;
    private int analysisWindowStep = 10;
    @Autowired
    private KnowledgeClient knowledgeClient;
    public void startAnalysis() {
        lastAnalysisTimestamp = new Date();
        //ci serve il set di servizi per ottenere la cofigurazione attuale
        List<Service> currentServices = knowledgeClient.getServices();

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
