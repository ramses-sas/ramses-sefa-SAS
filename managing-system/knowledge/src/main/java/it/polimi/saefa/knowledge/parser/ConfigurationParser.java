package it.polimi.saefa.knowledge.parser;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import it.polimi.saefa.configparser.ConfigProperty;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RestController
public class ConfigurationParser {
    @Autowired
    private EurekaClient discoveryClient;

    // <serviceId, configuration>
    //private final Map<String, ServiceConfiguration> serviceConfigurations = new HashMap<>();

    @GetMapping("/testParsing/{serviceId}") // TODO: remove annotation after test
    public ServiceConfiguration parseProperties(@PathVariable String serviceId) {
        InstanceInfo configInstance = getConfigServerInstance();
        String url = configInstance.getHomePageUrl() + "config-server/default/main/" + serviceId + ".properties";
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serviceId);
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                // se è una proprietà dei circuit breaker
                if (key.startsWith("resilience4j.circuitbreaker.")) {
                    String[] parts = keyValue[0].split("\\.");
                    String cbName = parts[parts.length-2];
                    String propName = parts[parts.length-1];
                    serviceConfiguration.addCircuitBreakerProperty(cbName, propName, value);
                    log.debug(serviceConfiguration.getCircuitBreakerConfigurations().get(cbName).toString());
                }
            } catch (Exception e) {
                log.error("Error parsing line {}", line);
            }
        }
        return serviceConfiguration;
    }

    @GetMapping("/testGlobalParsing") // TODO: remove annotation after test
    public void parseGlobalProperties(Map<String, Service> services) {
        InstanceInfo configInstance = getConfigServerInstance();
        String url = configInstance.getHomePageUrl() + "config-server/default/main/application.properties";
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration("application");
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                // se è una proprietà dei load balancer
                if (key.startsWith("loadbalancing.")) {
                    ConfigProperty configProperty = new ConfigProperty(key, value);
                    /*if (!services.containsKey(configProperty.getServiceId()))
                        serviceConfigurations.put(configProperty.getServiceId(), new ServiceConfiguration(configProperty.getServiceId()));
                     */
                    if (services.containsKey(configProperty.getServiceId())) {
                        ServiceConfiguration serviceToBalanceConfiguration = services.get(configProperty.getServiceId()).getConfiguration();
                        if (configProperty.getPropertyElements().length == 1 && configProperty.getPropertyElements()[0].equals("type"))
                            serviceToBalanceConfiguration.setLoadBalancerType(configProperty.getValue());
                        if (configProperty.getPropertyElements().length == 1 && configProperty.getPropertyElements()[0].equals("weight"))
                            serviceToBalanceConfiguration.addLoadBalancerWeight(configProperty.getAddress(), Integer.valueOf(configProperty.getValue()));
                    }
                }
                switch (key) {
                    // PARSE OTHER GLOBAL PROPERTIES HERE
                }
            } catch (Exception e) {
                log.error("Error parsing line {}, cause:{}", line, e.getMessage());
            }
        }
    }

    private InstanceInfo getConfigServerInstance() {
        return discoveryClient.getApplication("CONFIG-SERVER").getInstances().get(0);
    }

    /*@GetMapping("/refreshConfigurations")
    public String refreshConfigurations() {
        new Thread( () -> {
            parseGlobalProperties();
            for (String serviceId : instancesSupplier.getServicesInstances().keySet()) {
                parseProperties(serviceId);
            }
        }).start();
        return "OK";
    }
    @PostMapping("/configurationChanged")
    public String configurationChanged(@RequestBody String request) {
        Gson g = new Gson();
        String[] modifiedFiles = g.fromJson(g.fromJson(request, JsonObject.class)
                .getAsJsonObject("head_commit").get("modified"), String[].class);
        new Thread( () -> {
            for (String modifiedFile : modifiedFiles) {
                log.info("File " + modifiedFile + " changed");
                if (modifiedFile.equals("application.properties"))
                    parseGlobalProperties();
                else
                    parseProperties(modifiedFile.replace(".properties", ""));
            }
        }).start();
        return "OK";
    }*/


}
