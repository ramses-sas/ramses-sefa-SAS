package it.polimi.saefa.configparser;


import java.lang.reflect.InvocationTargetException;

public class ConfigParser<T> {
    private final String LBPREFIX = "loadbalancing.";
    private final T env;

    public ConfigParser(T env) {
        this.env = env;
    }

    public int getLoadBalancerWeight(String serviceId, String instanceId) {
        String prop = LBPREFIX+serviceId.toLowerCase()+"."+instanceId.replace(".","_").replace(":","_")+".weight";
        try {
            return Integer.parseInt(getProperty(prop));
        } catch (RuntimeException e) {
            // If the weight for that instance is not (correctly) set, return the default weight for that service
            return getLoadBalancerWeight(serviceId);
        }

    }

    // return the default weight to use for the service or 1 if not set
    public int getLoadBalancerWeight(String serviceId) {
        String prop = LBPREFIX+serviceId.toLowerCase()+".global.weight";
        return Integer.parseInt(getProperty(prop, "1"));
    }

    public String getLoadBalancerType(String serviceId) {
        String prop = LBPREFIX+serviceId.toLowerCase()+".global.type";
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
