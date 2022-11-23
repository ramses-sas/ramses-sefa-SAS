package it.polimi.ramses.dashboard.externalinterfaces;

import lombok.Getter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "ANALYSE", url = "${ANALYSE_URL}")
public interface AnalyseClient {

    @GetMapping("/rest/")
    GetInfoResponse getInfo();

    @PutMapping("/rest/metricsWindowSize")
    String changeMetricsWindowSize(@RequestParam int value);
    @PutMapping("/rest/analysisWindowSize")
    String changeAnalysisWindowSize(@RequestParam int value);
    @PutMapping("/rest/failureRateThreshold")
    String changeFailureRateThreshold(@RequestParam double value);
    @PutMapping("/rest/unreachableRateThreshold")
    String changeUnreachableRateThreshold(@RequestParam double value);
    @PutMapping("/rest/qoSSatisfactionRate")
    String changeQoSSatisfactionRate(@RequestParam double value);

    @Getter
    class GetInfoResponse {
        private int metricsWindowSize;
        private int analysisWindowSize;
        private double failureRateThreshold;
        private double unreachableRateThreshold;
        private double qosSatisfactionRate;
    }
}

