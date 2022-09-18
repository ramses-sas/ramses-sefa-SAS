package it.polimi.saefa.monitor.externalinterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ANALYSE", url = "${ANALYSE_URL}")
public interface AnalyseClient {
    @GetMapping("/beginAnalysis")
    String beginAnalysis();
}
