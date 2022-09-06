package it.polimi.saefa.configparser;

import java.util.Arrays;

public class ConfigProperty {
    final private String parentProperty;
    final private String serviceId;
    final private String address; // localhost_PORT oppure UNDERSCORE-SEPARATED-IP_PORT oppure global
    final private String[] propertyElements;
    final private String value;

    public ConfigProperty(String propertyKey, String value) {
        String[] propertyFields = propertyKey.split("\\.");
        this.parentProperty = propertyFields[0];
        this.serviceId = propertyFields[1];
        String instanceIdentifier = propertyFields[2];
        if (!instanceIdentifier.equals("global")) {
            int lastIndex = instanceIdentifier.lastIndexOf("_");
            if (lastIndex == -1) { throw new RuntimeException("Invalid address: " + instanceIdentifier); }
            String host = instanceIdentifier.substring(0, lastIndex).replace("_", ".");
            String port = instanceIdentifier.substring(lastIndex + 1);
            this.address = host + ":" + port;
        } else {
            this.address = instanceIdentifier;
        }
        this.propertyElements = Arrays.copyOfRange(propertyFields, 3, propertyFields.length);
        this.value = value;
    }

    public String getServiceId() {
        return serviceId.toUpperCase();
    }

    public boolean isGlobal() {
        return address.equals("global");
    }

    public String getParentProperty() {
        return parentProperty;
    }

    public String getAddress() {
        return address;
    }

    public String[] getPropertyElements() {
        return propertyElements;
    }

    public String getValue() {
        return value;
    }
}
