package it.polimi.ramses.analyse.rest;

import it.polimi.ramses.analyse.domain.AnalyseService;
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

    @PutMapping("/metricsWindowSize")
    public String changeMetricsWindowSize(@RequestParam int value) {
        analyseService.setNewMetricsWindowSize(value);
        return "OK";
    }

    @PutMapping("/analysisWindowSize")
    public String changeAnalysisWindowSize(@RequestParam int value) {
        analyseService.setNewAnalysisWindowSize(value);
        return "OK";
    }

    @PutMapping("/failureRateThreshold")
    public String changeFailureRateThreshold(@RequestParam double value) {
        analyseService.setNewFailureRateThreshold(value);
        return "OK";
    }

    @PutMapping("/unreachableRateThreshold")
    public String changeUnreachableRateThreshold(@RequestParam double value) {
        analyseService.setNewUnreachableRateThreshold(value);
        return "OK";
    }

    @PutMapping("/qoSSatisfactionRate")
    public String changeQoSSatisfactionRate(@RequestParam double value) {
        analyseService.setQosSatisfactionRate(value);
        return "OK";
    }
    
    @GetMapping("/")
    public GetInfoResponse getInfo() {
        return new GetInfoResponse(analyseService.getMetricsWindowSize(), analyseService.getAnalysisWindowSize(), 
                analyseService.getFailureRateThreshold(), analyseService.getUnreachableRateThreshold(), analyseService.getQosSatisfactionRate());
    }

    // TODO remove after test
    @GetMapping("/break")
    public String debug() {
        analyseService.breakpoint();
        return "Hello from Analysis Service";
    }

}
