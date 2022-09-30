package it.polimi.saefa.knowledge.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.AdaptationParamSpecification;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AdaptationParamParser {
    public static Map<String, List<AdaptationParamSpecification>> parse(Reader json){
        Map<String, List<AdaptationParamSpecification>> servicesAdaptationParameters = new HashMap<>();
        Gson gson = new Gson();
        JsonArray services = gson.fromJson(json, JsonObject.class).getAsJsonArray("services");
        services.forEach(service -> {
            List<AdaptationParamSpecification> adaptationParamSpecificationList = new LinkedList<>();
            JsonObject serviceJson = service.getAsJsonObject();
            String serviceId = serviceJson.get("service_id").getAsString();
            JsonArray adaptationParameters = serviceJson.get("adaptation_parameters").getAsJsonArray();
            adaptationParameters.forEach(param -> {
                JsonObject parameter = param.getAsJsonObject();
                String name = AdaptationParamSpecification.class.getPackage().getName() + "." + snakeToCamel(parameter.get("name").getAsString());
                Class<?> clazz;
                AdaptationParamSpecification adaptationParamSpecification;
                try {
                    clazz = Class.forName(name);
                    if (!AdaptationParamSpecification.class.isAssignableFrom(clazz))
                        throw new RuntimeException("The provided class " + clazz.getName() + " does not extend the AdaptationParameter class.");
                    adaptationParamSpecification = (AdaptationParamSpecification) clazz.getDeclaredConstructor(String.class).newInstance(parameter.toString());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                adaptationParamSpecificationList.add(adaptationParamSpecification);
            });
            if (adaptationParamSpecificationList.stream().map(AdaptationParamSpecification::getWeight).reduce(0.0, Double::sum)!=1)
                throw new RuntimeException("The sum of parameters weight for service " + serviceId + " should be equal to 1");
            servicesAdaptationParameters.put(serviceId, adaptationParamSpecificationList);
        });
        return servicesAdaptationParameters;
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
