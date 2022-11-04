package it.polimi.dummymanagedsystem.restclient.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class AdaptationController {
    @Value("${MONITOR_URL}")
    private String monitorURL;

    @Value("${PLAN_URL}")
    private String planURL;


    public void startMonitorRoutine() {
        String url = monitorURL+"/rest/startRoutine";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(url, String.class);
    }

    public void stopMonitorRoutine() {
        String url = monitorURL+"/rest/stopRoutine";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(url, String.class);
    }

    public void changeAdaptationStatus(boolean adapt) {
        String url = planURL+"/rest/adaptationStatus?adapt={adaptValue}";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(url, null, Map.of("adaptValue", String.valueOf(adapt)));
    }
}
