package it.polimi.saefa.restclient.domain;

import it.polimi.saefa.restclient.externalrestapi.RemoveInstanceRequest;
import it.polimi.saefa.restclient.externalrestapi.StartInstanceRequest;
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

    @Value("${DOCKER_ACTUATOR_URL}")
    private String dockerActuatorURL;


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

    public void setFakeCounter(int counter) {
        String url = probeURL+"/rest/fakeCounter?value={value}";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(url, null, Map.of("value", String.valueOf(counter)));
    }

    public void startInstance(String instanceId) {
        String url = dockerActuatorURL+"/rest/startInstance";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(url,
                new StartInstanceRequest(instanceId.split("@")[0],
                        instanceId.split("@")[1].split(":")[0],
                        Integer.parseInt(instanceId.split("@")[1].split(":")[1]))
                , String.class);
    }
    
    public void stopInstance(String instanceId) {
        String url = dockerActuatorURL+"/rest/removeInstance";
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(url, 
                new RemoveInstanceRequest(
                        instanceId.split("@")[0],
                        instanceId.split("@")[1].split(":")[0], 
                        Integer.parseInt(instanceId.split("@")[1].split(":")[1]))
                , String.class);
    }

}
