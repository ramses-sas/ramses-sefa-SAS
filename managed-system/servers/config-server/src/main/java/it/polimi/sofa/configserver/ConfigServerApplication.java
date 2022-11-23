package it.polimi.sofa.configserver;

import com.google.gson.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@EnableConfigServer
@EnableEurekaClient
@RestController
@SpringBootApplication
public class ConfigServerApplication {
    private final Logger log = Logger.getLogger(ConfigServerApplication.class.getName());

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }

    @Autowired
    private DiscoveryClient discoveryClient;

    @Value("${spring.application.name}")
    private String appName;

    @PostMapping(value = "/refreshProperties")
    public String refreshProperties(@RequestBody String request) {
        Gson g = new Gson();
        String[] modifiedFiles = g.fromJson(g.fromJson(request, JsonObject.class)
                                  .getAsJsonObject("head_commit").get("modified"), String[].class);
        for (String modifiedFile : modifiedFiles) {
            log.info("File " + modifiedFile + " changed");
            if (modifiedFile.equals("application.properties")) {
                for (ServiceInstance instance: getInstances(null)) {
                    notifyInstance(instance);
                }
            } else {
                String serviceId = modifiedFile.replace(".properties", "");
                for (ServiceInstance instance: getInstances(serviceId)) {
                    notifyInstance(instance);
                }
            }
        }
        return "OK";
    }

    private void notifyInstance(ServiceInstance instance) {
        log.info("Notifying " + instance.getServiceId() + "@" + instance.getHost() + ":" + instance.getPort());
        String scheme = instance.getScheme() == null ? "http" : instance.getScheme();
        URI url = URI.create(scheme + "://" + instance.getHost() + ":" + instance.getPort() + "/actuator/refresh");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpClient client = HttpClient.newBuilder().build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(t -> {
                    log.severe("Error refreshing " + instance.getServiceId() + "@" + instance.getHost() + ":" + instance.getPort() + ". Error: " + t.getMessage());
                    return null;
                })
                .thenAccept(body -> log.info("Refreshed " + instance.getServiceId() + "@" + instance.getHost() + ":" + instance.getPort()));
    }

    private List<ServiceInstance> getInstances(String serviceId) {
        if (serviceId != null) {
            return discoveryClient.getInstances(serviceId);
        }
        List<String> services = discoveryClient.getServices();
        List<ServiceInstance> instances = new ArrayList<>();
        services.forEach(serviceName -> {
            if (!serviceName.equalsIgnoreCase(this.appName)) {
                instances.addAll(discoveryClient.getInstances(serviceName));
            }
        });
        return instances;
    }

}

