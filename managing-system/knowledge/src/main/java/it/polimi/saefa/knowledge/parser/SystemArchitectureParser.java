package it.polimi.saefa.knowledge.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.saefa.knowledge.domain.architecture.Service;
import it.polimi.saefa.knowledge.domain.architecture.ServiceImplementation;

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
                double costPerInstance = implementation.get("cost_per_instance").getAsDouble();
                double costPerRequest = implementation.get("cost_per_request").getAsDouble();
                double costPerSecond = implementation.get("cost_per_second").getAsDouble();
                double costPerBoot = implementation.get("cost_per_boot").getAsDouble();
                double score = implementation.get("score").getAsDouble();
                double riskFactor = implementation.get("risk_factor").getAsDouble();
                serviceImplementations.add(new ServiceImplementation(implementationId, costPerInstance, costPerRequest, costPerSecond, costPerBoot, score, riskFactor));
            });
            double totalScore = serviceImplementations.stream().map(ServiceImplementation::getScore).reduce(0.0, Double::sum);
            if (totalScore != 1.0) {
                throw new RuntimeException("Total score of service " + serviceId + " is not 1.0");
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
