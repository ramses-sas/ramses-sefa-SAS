package it.polimi.saefa.configparser;

public class CustomPropertiesReader<T> {
    private final String LBPREFIX = "loadbalancing.";
    private final T env;

    public CustomPropertiesReader(T env) {
        this.env = env;
    }

    public int getLoadBalancerInstanceWeight(String serviceId, String address) {
        String prop = LBPREFIX+serviceId.toLowerCase()+"."+address.replace(".","_").replace(":","_")+".weight";
        try {
            return Integer.parseInt(getProperty(prop));
        } catch (RuntimeException e) {
            // If the weight for that instance is not (correctly) set, return the default weight for that service
            return getLoadBalancerServiceDefaultWeight(serviceId);
        }

    }

    // return the default weight to use for the service or 1 if not set
    public int getLoadBalancerServiceDefaultWeight(String serviceId) {
        String prop = LBPREFIX+serviceId.toLowerCase()+".global.weight";
        return Integer.parseInt(getProperty(prop, "1"));
    }

    public String getLoadBalancerTypeOrDefault(String serviceId) {
        String prop = LBPREFIX+serviceId.toLowerCase()+".global.type";
        return getProperty(prop, getProperty(LBPREFIX+".global.type", "ROUND_ROBIN"));
    }

    public CustomProperty parse(String propertyKey) {
        return new CustomProperty(propertyKey, getProperty(propertyKey));
    }

    String getProperty(String key, String defaultValue) {
        try {
            return (String) env.getClass().getMethod("getProperty", String.class, String.class).invoke(env, key, defaultValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    String getProperty(String key) {
        try {
            return (String) env.getClass().getMethod("getProperty", String.class).invoke(env, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
