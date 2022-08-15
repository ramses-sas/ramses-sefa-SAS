package polimi.saefa.configparser;

import org.springframework.core.env.Environment;


public class ConfigParser {

    private Environment env;

    public ConfigParser(Environment env) {
        this.env = env;
    }

    public int getLBWeight(String serviceId, String instanceId) {
        String prop = "loadbalancing."+serviceId.toLowerCase()+"."+instanceId.replace(".","_").replace(":","_")+".weight";
        return Integer.parseInt(env.getProperty(prop, "1"));
    }

    public int getLBWeight(String serviceId) {
        String prop = "loadbalancing."+serviceId.toLowerCase()+".global.weight";
        return Integer.parseInt(env.getProperty(prop, "1"));
    }

    public String getLBType(String serviceId) {
        String prop = "loadbalancing."+serviceId.toLowerCase()+".global.type";
        return env.getProperty(prop, "ROUND_ROBIN");
    }

    public ConfigProperty parse(String propertyKey) {
        return new ConfigProperty(propertyKey, env.getProperty(propertyKey));
    }

}
