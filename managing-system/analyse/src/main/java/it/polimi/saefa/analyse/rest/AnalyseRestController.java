package it.polimi.saefa.analyse.rest;

import it.polimi.saefa.analyse.domain.AnalyseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
}
