package it.polimi.saefa.configparser;


import java.lang.reflect.InvocationTargetException;

public class ConfigParser<T> {

    private final T env;

    public ConfigParser(T env) {
        this.env = env;
    }

    public int getLBWeight(String serviceId, String instanceId) {
        String prop = "loadbalancing."+serviceId.toLowerCase()+"."+instanceId.replace(".","_").replace(":","_")+".weight";
        return Integer.parseInt(getProperty(prop, "1"));
    }

    public int getLBWeight(String serviceId) {
        String prop = "loadbalancing."+serviceId.toLowerCase()+".global.weight";
        return Integer.parseInt(getProperty(prop, "1"));
    }

    public String getLBType(String serviceId) {
        String prop = "loadbalancing."+serviceId.toLowerCase()+".global.type";
        return getProperty(prop, "ROUND_ROBIN");
    }

    public ConfigProperty parse(String propertyKey) {
        return new ConfigProperty(propertyKey, getProperty(propertyKey));
    }

    String getProperty(String key, String defaultValue) {
        try {
            return (String) env.getClass().getMethod("getProperty", String.class, String.class).invoke(env, key, defaultValue);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    String getProperty(String key) {
        try {
            return (String) env.getClass().getMethod("getProperty", String.class).invoke(env, key);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
