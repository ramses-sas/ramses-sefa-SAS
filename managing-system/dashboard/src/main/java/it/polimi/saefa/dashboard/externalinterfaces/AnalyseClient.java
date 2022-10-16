package it.polimi.saefa.dashboard.externalinterfaces;

import lombok.Getter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "ANALYSE", url = "${ANALYSE_URL}")
public interface AnalyseClient {

    @GetMapping("/rest/")
    GetInfoResponse getInfo();

    @PutMapping("/rest/changeMetricsWindowSize")
    String changeMetricsWindowSize(@RequestParam int value);
    @PutMapping("/rest/changeAnalysisWindowSize")
    String changeAnalysisWindowSize(@RequestParam int value);
    @PutMapping("/rest/changeFailureRateThreshold")
    String changeFailureRateThreshold(@RequestParam double value);
    @PutMapping("/rest/changeUnreachableRateThreshold")
    String changeUnreachableRateThreshold(@RequestParam double value);
    @PutMapping("/rest/changeParametersSatisfactionRate")
    String changeParametersSatisfactionRate(@RequestParam double value);

    @Getter
    class GetInfoResponse {
        private int metricsWindowSize;
        private int analysisWindowSize;
        private double failureRateThreshold;
        private double unreachableRateThreshold;
        private double parametersSatisfactionRate;
    }
}

