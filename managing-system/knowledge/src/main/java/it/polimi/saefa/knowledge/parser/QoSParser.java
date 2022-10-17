package it.polimi.saefa.knowledge.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import it.polimi.saefa.knowledge.domain.adaptation.specifications.QoSSpecification;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QoSParser {
    public static Map<String, List<QoSSpecification>> parse(Reader json){
        Map<String, List<QoSSpecification>> servicesQoS = new HashMap<>();
        Gson gson = new Gson();
        JsonArray services = gson.fromJson(json, JsonObject.class).getAsJsonArray("services");
        services.forEach(service -> {
            List<QoSSpecification> qoSSpecificationList = new LinkedList<>();
            JsonObject serviceJson = service.getAsJsonObject();
            String serviceId = serviceJson.get("service_id").getAsString();
            JsonArray qoSJSON = serviceJson.get("qos").getAsJsonArray();
            qoSJSON.forEach(param -> {
                JsonObject parameter = param.getAsJsonObject();
                String name = QoSSpecification.class.getPackage().getName() + "." + snakeToCamel(parameter.get("name").getAsString());
                Class<?> clazz;
                QoSSpecification qoSSpecification;
                try {
                    clazz = Class.forName(name);
                    if (!QoSSpecification.class.isAssignableFrom(clazz))
                        throw new RuntimeException("The provided class " + clazz.getName() + " does not extend the QoSSpecification class.");
                    qoSSpecification = (QoSSpecification) clazz.getDeclaredConstructor(String.class).newInstance(parameter.toString());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                qoSSpecificationList.add(qoSSpecification);
            });
            if (qoSSpecificationList.stream().map(QoSSpecification::getWeight).reduce(0.0, Double::sum)!=1)
                throw new RuntimeException("The sum of QoS weights for service " + serviceId + " should be equal to 1");
            servicesQoS.put(serviceId, qoSSpecificationList);
        });
        return servicesQoS;
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
