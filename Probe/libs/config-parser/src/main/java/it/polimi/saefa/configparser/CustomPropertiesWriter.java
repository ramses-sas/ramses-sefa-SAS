package it.polimi.saefa.configparser;

import static it.polimi.saefa.configparser.CustomPropertiesDefinition.LBPREFIX;

public class CustomPropertiesWriter {

    public static String buildLoadBalancerInstanceWeightPropertyKey(String serviceId, String address) {
        return LBPREFIX+serviceId.toLowerCase()+"."+address.replace(".","_").replace(":","_")+".weight";
    }

    public static String buildLoadBalancerTypePropertyKey(String serviceId) {
        return LBPREFIX+serviceId.toLowerCase()+".global.type";
    }

}
