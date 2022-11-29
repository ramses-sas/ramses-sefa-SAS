package it.polimi.sefa.restclient.domain;

import it.polimi.sefa.restclient.externalrestapi.RemoveInstanceRequest;
import it.polimi.sefa.restclient.externalrestapi.StartInstanceRequest;
import it.polimi.sefa.restclient.externalrestapi.UpdateBenchmarkRequest;
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

    @Value("${PROBE_URL}")
    private String probeURL;

    @Value("${KNOWLEDGE_URL}")
    private String knowledgeURL;

    @Value("${DOCKER_ACTUATOR_URL}")
    private String dockerActuatorURL;


    public void startMonitorRoutine() {
        String url = monitorURL+"/rest/startRoutine";
        log.info("startMonitorRoutine request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(url, String.class);
    }

    public void stopMonitorRoutine() {
        String url = monitorURL+"/rest/stopRoutine";
        log.info("stopMonitorRoutine request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject(url, String.class);
    }

    public void changeAdaptationStatus(boolean adapt) {
        String url = planURL+"/rest/adaptationStatus?adapt={adaptValue}";
        log.info("changeAdaptationStatus request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(url, null, Map.of("adaptValue", String.valueOf(adapt)));
    }

    public void setFakeCounter(int counter) {
        String url = probeURL+"/rest/fakeCounter?value={value}";
        log.info("setFakeCounter request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(url, null, Map.of("value", String.valueOf(counter)));
    }

    public void startInstance(String instanceId) {
        String url = dockerActuatorURL+"/rest/startInstance";
        log.info("startInstance request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(url,
                new StartInstanceRequest(instanceId.split("@")[0],
                        instanceId.split("@")[1].split(":")[0],
                        Integer.parseInt(instanceId.split("@")[1].split(":")[1]))
                , String.class);
    }
    
    public void stopInstance(String instanceId) {
        String url = dockerActuatorURL+"/rest/removeInstance";
        log.info("stopInstance request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(url, 
                new RemoveInstanceRequest(
                        instanceId.split("@")[0],
                        instanceId.split("@")[1].split(":")[0], 
                        Integer.parseInt(instanceId.split("@")[1].split(":")[1]))
                , String.class);
    }


    public void updateBenchmarks(String serviceId, String serviceImplementationId, String qosClass, Double value) {
        String url = knowledgeURL+"/rest/service/{serviceId}/updateBenchmarks";
        log.info("updateBenchmarks request: {}", url);
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(url,
                new UpdateBenchmarkRequest(serviceImplementationId, qosClass, value),
                String.class,
                Map.of("serviceId", serviceId));
    }

}
