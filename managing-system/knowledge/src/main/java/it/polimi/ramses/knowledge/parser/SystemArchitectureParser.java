package it.polimi.ramses.knowledge.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.ramses.knowledge.domain.architecture.Service;
import it.polimi.ramses.knowledge.domain.architecture.ServiceImplementation;

import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

public class SystemArchitectureParser {
    public static List<Service> parse(Reader json) {
        List<Service> serviceList = new LinkedList<>();
        Gson g = new Gson();
        JsonArray services = g.fromJson(json, JsonObject.class).getAsJsonArray("services");
        services.forEach(service -> {
            JsonObject serviceJson = service.getAsJsonObject();
            String serviceId = serviceJson.get("service_id").getAsString();
            JsonArray implementations = serviceJson.get("implementations").getAsJsonArray();

            List<ServiceImplementation> serviceImplementations = new LinkedList<>();
            implementations.forEach(impl -> {
                JsonObject implementation = impl.getAsJsonObject();
                String implementationId = implementation.get("implementation_id").getAsString();
                double preference = implementation.get("preference").getAsDouble();
                int implementationTrust = implementation.get("implementation_trust").getAsInt();
                double instanceLoadShutdownThreshold = implementation.get("instance_load_shutdown_threshold") == null ? 0 : implementation.get("instance_load_shutdown_threshold").getAsDouble();
                serviceImplementations.add(new ServiceImplementation(implementationId, preference, implementationTrust, instanceLoadShutdownThreshold));
            });
            double totalPreference = serviceImplementations.stream().map(ServiceImplementation::getPreference).reduce(0.0, Double::sum);
            if (totalPreference != 1.0) {
                throw new RuntimeException("Total preference of service " + serviceId + " is not 1.0");
            }
            JsonArray dependencies = serviceJson.get("dependencies").getAsJsonArray();
            List<String> serviceDependencies = new LinkedList<>();
            dependencies.forEach(dep -> {
                JsonObject dependency = dep.getAsJsonObject();
                String dependencyName = dependency.get("name").getAsString();
                serviceDependencies.add(dependencyName);
            });
            Service s = new Service(serviceId, serviceImplementations, serviceDependencies);
            serviceList.add(s);
        });
        return serviceList;
    }
}
