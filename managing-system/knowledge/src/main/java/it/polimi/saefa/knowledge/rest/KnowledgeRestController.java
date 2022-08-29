package it.polimi.saefa.knowledge.rest;


import it.polimi.saefa.knowledge.persistence.InstanceMetrics;
import it.polimi.saefa.knowledge.persistence.PersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="/rest/")
public class KnowledgeRestController {
    @Autowired
    private PersistenceService persistenceService;

    @PostMapping("/")
    public String addMetric(InstanceMetrics metrics) {
        persistenceService.addMetrics(metrics);
        return "OK";
    }
}
