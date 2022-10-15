package it.polimi.saefa.analyse.rest;

import it.polimi.saefa.analyse.domain.AnalyseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/rest")
public class AnalyseRestController {
    @Autowired
    private AnalyseService analyseService;

    @GetMapping("/start")
    public String start() {
        (new Thread(() -> analyseService.startAnalysis())).start();
        return "OK";
    }

    @PutMapping("/changeMetricsWindowSize")
    public String changeMetricsWindowSize(@RequestParam int value) {
        analyseService.setNewMetricsWindowSize(value);
        return "OK";
    }

    @PutMapping("/changeAnalysisWindowSize")
    public String changeAnalysisWindowSize(@RequestParam int value) {
        analyseService.setNewAnalysisWindowSize(value);
        return "OK";
    }

    @PutMapping("/changeFailureRateThreshold")
    public String changeFailureRateThreshold(@RequestParam double value) {
        analyseService.setNewFailureRateThreshold(value);
        return "OK";
    }

    @PutMapping("/changeUnreachableRateThreshold")
    public String changeUnreachableRateThreshold(@RequestParam double value) {
        analyseService.setNewUnreachableRateThreshold(value);
        return "OK";
    }

    @PutMapping("/changeParametersSatisfactionRate")
    public String changeParametersSatisfactionRate(@RequestParam double value) {
        analyseService.setParametersSatisfactionRate(value);
        return "OK";
    }
    
    @GetMapping("/")
    public GetInfoResponse getInfo() {
        return new GetInfoResponse(analyseService.getMetricsWindowSize(), analyseService.getAnalysisWindowSize(), 
                analyseService.getFailureRateThreshold(), analyseService.getUnreachableRateThreshold(), analyseService.getParametersSatisfactionRate());
    }

    // TODO remove after test
    @GetMapping("/break")
    public String debug() {
        analyseService.breakpoint();
        return "Hello from Analysis Service";
    }

}
