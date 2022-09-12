package it.polimi.saefa.monitor.externalinterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ANALYSE", url = "http://localhost:58002")
public interface AnalyseClient {
    @GetMapping("/beginAnalysis")
    String beginAnalysis();
}
