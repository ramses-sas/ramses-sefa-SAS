package it.polimi.saefa.knowledge.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.saefa.knowledge.persistence.domain.adaptation.AdaptationParameter;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AdaptationParametersParser {
    public static Map<String, List<AdaptationParameter>> parse(Reader json){
        Map<String, List<AdaptationParameter>> serviceAdaptationParameters = new HashMap<>();
        Gson gson = new Gson();
        JsonArray services = gson.fromJson(json, JsonObject.class).getAsJsonArray("services");
        services.forEach(service -> {
            List<AdaptationParameter> adaptationParameterList = new LinkedList<>();
            JsonObject serviceJson = service.getAsJsonObject();
            String serviceId = serviceJson.get("service_id").getAsString();
            JsonArray adaptationParameters = serviceJson.get("adaptation_parameters").getAsJsonArray();
            adaptationParameters.forEach(param -> {
                JsonObject parameter = param.getAsJsonObject();
                String name = "it.polimi.saefa.knowledge.persistence.domain.adaptation." + snakeToCamel(parameter.get("name").getAsString());
                Class<?> clazz;
                AdaptationParameter adaptationParameter;
                try {
                    clazz = Class.forName(name);
                    if(!AdaptationParameter.class.isAssignableFrom(clazz))
                        throw new RuntimeException("The provided class " + clazz.getName() + " does not extend the AdaptationParameter class.");
                    adaptationParameter = (AdaptationParameter) clazz.getDeclaredConstructor(String.class).newInstance(parameter.toString());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException |
                        NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                adaptationParameterList.add(adaptationParameter);
            });
            if(adaptationParameterList.stream().map(AdaptationParameter::getWeight).reduce(0.0, Double::sum)!=1)
                throw new RuntimeException("The sum of parameters weight for service " + serviceId + " should be equal to 1");
            serviceAdaptationParameters.put(serviceId, adaptationParameterList);
        });
        return serviceAdaptationParameters;
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
