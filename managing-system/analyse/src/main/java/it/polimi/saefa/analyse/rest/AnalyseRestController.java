package it.polimi.saefa.analyse.rest;

import it.polimi.saefa.analyse.domain.AnalyseService;
import it.polimi.saefa.analyse.externalInterfaces.KnowledgeClient;
import it.polimi.saefa.knowledge.domain.adaptation.options.AdaptationOption;
import it.polimi.saefa.knowledge.domain.adaptation.options.AddInstances;
import it.polimi.saefa.knowledge.domain.adaptation.options.RemoveInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest")
public class AnalyseRestController {
    @Autowired
    private AnalyseService analyseService;

    @Autowired
    private KnowledgeClient knowledgeClient;

    @GetMapping("/start")
    public String start() {
        (new Thread(() -> analyseService.startAnalysis())).start();
        return "OK";
    }

    @PostMapping("/changeMetricsWindow")
    public String changeMetricsWindow(@RequestBody ChangeParameterRequest request) {
        analyseService.setNewMetricsWindowSize((int) request.getValue());
        return "OK";
    }

    @PostMapping("/changeAnalysisWindowSize")
    public String changeAnalysisWindowSize(@RequestBody ChangeParameterRequest request) {
        analyseService.setNewAnalysisWindowSize((int) request.getValue());
        return "OK";
    }

    @PostMapping("/changeFailureThreshold")
    public String changeFailureThreshold(@RequestBody ChangeParameterRequest request) {
        analyseService.setNewFailureRateThreshold(request.getValue());
        return "OK";
    }

    @PostMapping("/changeUnreachableThreshold")
    public String changeUnreachableThreshold(@RequestBody ChangeParameterRequest request) {
        analyseService.setNewUnreachableRateThreshold(request.getValue());
        return "OK";
    }

    // TODO remove after test
    @GetMapping("/test")
    public String test() {
        List<AdaptationOption> opt = List.of(new AddInstances("restaurant-service", "restaurant-service", 1, "test"), new RemoveInstance("restaurant-service", "restaurant-service", "restaurant-service@localhost:58085", "test"));
        knowledgeClient.proposeAdaptationOptions(opt);
        knowledgeClient.chooseAdaptationOptions(opt);
        return "OK";
    }
}
