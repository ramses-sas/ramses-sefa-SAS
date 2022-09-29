package it.polimi.saefa.knowledge.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.specifications.AdaptationParamSpecification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SystemBenchmarkParser {
    public static Map<String, List<ServiceImplementationBenchmarks>> parse(Reader json){
        Map<String, List<ServiceImplementationBenchmarks>> servicesBenchmarks = new HashMap<>();
        Gson gson = new Gson();
        JsonArray services = gson.fromJson(json, JsonObject.class).getAsJsonArray("services");
        services.forEach(service -> {
            List<ServiceImplementationBenchmarks> serviceImplementationsBenchmarks = new LinkedList<>();
            JsonObject serviceJson = service.getAsJsonObject();
            String serviceId = serviceJson.get("service_id").getAsString();
            JsonArray implementations = serviceJson.get("implementations").getAsJsonArray();
            implementations.forEach(impl -> {
                JsonObject implementation = impl.getAsJsonObject();
                String serviceImplementationId = implementation.get("implementation_id").getAsString();
                JsonArray adaptationBenchmarks = implementation.get("adaptation_benchmarks").getAsJsonArray();
                ServiceImplementationBenchmarks serviceImplementationBenchmarks = new ServiceImplementationBenchmarks(serviceId, serviceImplementationId);
                adaptationBenchmarks.forEach(adaptationBenchmark -> {
                    JsonObject adaptationBenchmarkJson = adaptationBenchmark.getAsJsonObject();
                    String adaptationParamSpecificationClassName = AdaptationParamSpecification.class.getPackage().getName() + "." + snakeToCamel(adaptationBenchmarkJson.get("name").getAsString());
                    Class<?> clazz;
                    Double benchmark = adaptationBenchmarkJson.get("benchmark").getAsDouble();
                    try {
                        clazz = Class.forName(adaptationParamSpecificationClassName);
                        if (!AdaptationParamSpecification.class.isAssignableFrom(clazz))
                            throw new RuntimeException("The provided class " + clazz.getName() + " does not extend the AdaptationParameter class.");
                        serviceImplementationBenchmarks.getAdaptationParametersBenchmarks().put((Class<? extends AdaptationParamSpecification>) clazz, benchmark);

                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
                serviceImplementationsBenchmarks.add(serviceImplementationBenchmarks);
            });
            servicesBenchmarks.put(serviceId, serviceImplementationsBenchmarks);
        });
        return servicesBenchmarks;
    }

    @Getter
    public static class ServiceImplementationBenchmarks{
        private final String serviceId;
        private final String serviceImplementationId;
        private final Map<Class<? extends AdaptationParamSpecification>, Double> adaptationParametersBenchmarks = new HashMap<>();

        public ServiceImplementationBenchmarks(String serviceId, String serviceImplementationId) {
            this.serviceId = serviceId;
            this.serviceImplementationId = serviceImplementationId;
        }
    }
    private static String snakeToCamel(String str) {
        str = str.substring(0, 1).toUpperCase() + str.substring(1);
        StringBuilder builder = new StringBuilder(str);
        for (int i = 0; i < builder.length(); i++) {
            if (builder.charAt(i) == '_') {
                builder.deleteCharAt(i);
                builder.replace(i, i + 1, String.valueOf(Character.toUpperCase(builder.charAt(i))));
            }
        }
        return builder.toString();
    }
}
