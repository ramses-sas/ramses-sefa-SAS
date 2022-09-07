package it.polimi.saefa.knowledge;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.saefa.knowledge.persistence.KnowledgeService;
import it.polimi.saefa.knowledge.persistence.domain.architecture.Service;
import it.polimi.saefa.knowledge.persistence.domain.architecture.ServiceImplementation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Component
public class KnowledgeInit implements CommandLineRunner {
    @Autowired
    private KnowledgeService knowledgeService;

    @Override
    public void run(String... args) throws Exception {
        FileReader reader = new FileReader(ResourceUtils.getFile("classpath:system_architecture.json"));
        Gson g = new Gson();
        JsonArray services = g.fromJson(reader, JsonObject.class).getAsJsonArray("services");
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
                serviceImplementations.add(new ServiceImplementation(implementationId, costPerInstance, costPerRequest, costPerSecond, costPerBoot, score));
            });
            double totalScore = serviceImplementations.stream().map(ServiceImplementation::getScore).reduce(0.0, Double::sum);
            if (totalScore != 1.0) {
                throw new RuntimeException("Total score of service " + serviceId + " is not 1.0");
            }
            Service s = new Service(serviceId, serviceImplementations);

            knowledgeService.addService(s);
        });
        log.info("Knowledge initialized");
    }
}
