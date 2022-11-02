package it.polimi.saefa.probe.configuration;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import it.polimi.saefa.configparser.CustomProperty;
import it.polimi.saefa.probe.domain.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class ConfigurationParser {
    @Autowired
    private EurekaClient discoveryClient;


    public ServiceConfiguration parsePropertiesAndCreateConfiguration(String serviceId, String serviceImplementationId) {
        InstanceInfo configInstance = getConfigServerInstance();
        String url = configInstance.getHomePageUrl() + "config-server/default/main/" + serviceId.toLowerCase() + ".properties";
        log.debug("Fetching configuration from " + url);
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(serviceId);
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                // se è una proprietà dei circuit breaker
                if (key.startsWith("resilience4j.circuitbreaker.") && !key.endsWith("ignoreExceptions")) {
                    String[] parts = keyValue[0].split("\\.");
                    String cbName = parts[parts.length-2];
                    String propName = parts[parts.length-1];
                    serviceConfiguration.addCircuitBreakerProperty(cbName, propName, value);
                }
            } catch (Exception e) {
                log.error("Error parsing line {}", line);
                log.error(e.getMessage());
            }
        }
        serviceConfiguration.setTimestamp(new Date());
        return parseGlobalProperties(serviceConfiguration, serviceId, serviceImplementationId);
    }

//TODO non serve globale a sto punto, no? Fare un solo metodo
    /*
    public void parseGlobalProperties(Map<String, Service> services) {
        InstanceInfo configInstance = getConfigServerInstance();
        String url = configInstance.getHomePageUrl() + "config-server/default/main/application.properties";
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                // se è una proprietà dei load balancer
                if (key.startsWith("loadbalancing.")) {
                    CustomProperty customProperty = new CustomProperty(key, value);
                    if (customProperty.getPropertyElements().length == 1) {
                        String propertyName = customProperty.getPropertyElements()[0];
                        if (customProperty.isServiceGlobal() && propertyName.equals("type")) {
                            for (Service service : services.values())
                                service.getConfiguration().setLoadBalancerType(customProperty.getValue().equalsIgnoreCase("weighted_random") ? ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM : ServiceConfiguration.LoadBalancerType.UNKNOWN);
                        } else if (services.containsKey(customProperty.getServiceId())) {
                            ServiceConfiguration serviceToBalanceConfiguration = services.get(customProperty.getServiceId()).getConfiguration();
                            switch (propertyName) {
                                case "type" ->
                                    serviceToBalanceConfiguration.setLoadBalancerType(customProperty.getValue().equalsIgnoreCase("weighted_random") ? ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM : ServiceConfiguration.LoadBalancerType.UNKNOWN);
                                case "weight" ->
                                    serviceToBalanceConfiguration.addLoadBalancerWeight(services.get(serviceToBalanceConfiguration.getServiceId()).getCurrentImplementationId() + "@" + customProperty.getAddress(), Double.valueOf(customProperty.getValue()));
                            }
                        }
                    }
                }
                switch (key) {
                    // PARSE OTHER GLOBAL PROPERTIES HERE
                }
            } catch (Exception e) {
                log.error("Error parsing line {}, cause: {}", line, e.getMessage());
            }
        }
    }
     */
    public ServiceConfiguration parseGlobalProperties(ServiceConfiguration configuration, String serviceId, String serviceImplementationId) {
        InstanceInfo configInstance = getConfigServerInstance();
        String url = configInstance.getHomePageUrl() + "config-server/default/main/application.properties";
        ResponseEntity<String> response = new RestTemplate().getForEntity(url, String.class);
        String[] lines = Arrays.stream(response.getBody().split("\n")).filter(line -> line.matches("([\\w\\.-])+=.+")).toArray(String[]::new);
        for (String line : lines) {
            try {
                String[] keyValue = line.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                // se è una proprietà dei load balancer
                if (key.startsWith("loadbalancing.")) {
                    CustomProperty customProperty = new CustomProperty(key, value);
                    if (customProperty.getPropertyElements().length == 1) {
                        String propertyName = customProperty.getPropertyElements()[0];
                        if (customProperty.isServiceGlobal() && propertyName.equals("type")) {
                            configuration.setLoadBalancerType(customProperty.getValue().equalsIgnoreCase("weighted_random") ? ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM : ServiceConfiguration.LoadBalancerType.UNKNOWN);
                        } else if (serviceId.equals(customProperty.getServiceId())) {
                            switch (propertyName) {
                                case "type" ->
                                        configuration.setLoadBalancerType(customProperty.getValue().equalsIgnoreCase("weighted_random") ? ServiceConfiguration.LoadBalancerType.WEIGHTED_RANDOM : ServiceConfiguration.LoadBalancerType.UNKNOWN);
                                case "weight" ->
                                        configuration.addLoadBalancerWeight(serviceImplementationId + "@" + customProperty.getAddress(), Double.valueOf(customProperty.getValue()));
                            }
                        }
                    }
                }
                switch (key) {
                    // PARSE OTHER GLOBAL PROPERTIES HERE
                }
            } catch (Exception e) {
                log.error("Error parsing line {}, cause: {}", line, e.getMessage());
            }
        }
        return configuration;
    }



    private InstanceInfo getConfigServerInstance() {
        return discoveryClient.getApplication("CONFIG-SERVER").getInstances().get(0);
    }

    /*@GetMapping("/refreshConfigurations")
    public String refreshConfigurations() {
        new Thread( () -> {
            parseGlobalProperties();
            for (String serviceId : instancesSupplier.getServicesInstances().keySet()) {
                parsePropertiesAndCreateConfiguration(serviceId);
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
                    parsePropertiesAndCreateConfiguration(modifiedFile.replace(".properties", ""));
            }
        }).start();
        return "OK";
    }*/


}
