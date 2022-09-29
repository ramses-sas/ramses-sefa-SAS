package it.polimi.saefa.configparser;

import static it.polimi.saefa.configparser.CustomPropertiesDefinition.LBPREFIX;

public class CustomPropertiesWriter {

    public String setLoadBalancerInstanceWeight(String serviceId, String address, String value) {
        String key = LBPREFIX+serviceId.toLowerCase()+"."+address.replace(".","_").replace(":","_")+".weight";
        return key+"="+value;
    }

    public String setLoadBalancerType(String serviceId, String value) {
        String key = LBPREFIX+serviceId.toLowerCase()+".global.type";
        return key+"="+value;
    }

}
