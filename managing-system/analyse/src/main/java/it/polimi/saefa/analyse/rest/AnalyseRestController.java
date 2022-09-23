package it.polimi.saefa.analyse.rest;

import it.polimi.saefa.analyse.domain.AnalyseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyseRestController {
    @Autowired
    private AnalyseService analyseService;

    @GetMapping("/beginAnalysis")
    public String beginAnalysis() {
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

}
