package it.polimi.ramses.configparser;

public class CustomPropertiesWriter {

    public static String buildLoadBalancerInstanceWeightPropertyKey(String serviceId, String address) {
        return CustomPropertiesDefinition.LBPREFIX+serviceId.toLowerCase()+"."+address.replace(".","_").replace(":","_")+".weight";
    }

    public static String buildLoadBalancerTypePropertyKey(String serviceId) {
        return CustomPropertiesDefinition.LBPREFIX+serviceId.toLowerCase()+".global.type";
    }

}
