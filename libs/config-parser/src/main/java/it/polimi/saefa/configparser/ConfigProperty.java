package it.polimi.saefa.configparser;

import java.util.Arrays;

public class ConfigProperty {
    final private String parentProperty;
    final private String serviceId;
    final private String address; // Nel CONFIG deve essere specificato come localhost_PORT oppure UNDERSCORE-SEPARATED-IP_PORT oppure global
    final private String[] propertyElements;
    final private String value;

    public ConfigProperty(String propertyKey, String value) {
        if (propertyKey == null || value == null)
            throw new IllegalArgumentException("Property key and value cannot be null");
        String[] propertyFields = propertyKey.split("\\.");
        this.parentProperty = propertyFields[0];
        this.serviceId = propertyFields[1];
        if (serviceId.equals("global")) {
            this.address = "global";
            this.propertyElements = Arrays.copyOfRange(propertyFields, 2, propertyFields.length);
        } else {
            String instanceIdentifier = propertyFields[2];
            if (!instanceIdentifier.equals("global")) {
                int lastIndex = instanceIdentifier.lastIndexOf("_");
                if (lastIndex == -1) {
                    throw new RuntimeException("Invalid address: " + instanceIdentifier);
                }
                String host = instanceIdentifier.substring(0, lastIndex).replace("_", ".");
                String port = instanceIdentifier.substring(lastIndex + 1);
                this.address = host + ":" + port;
            } else {
                this.address = instanceIdentifier;
            }
            this.propertyElements = Arrays.copyOfRange(propertyFields, 3, propertyFields.length);
        }
        this.value = value;
    }

    public String getServiceId() {
        return serviceId.toUpperCase();
    }

    public boolean isServiceGlobal() {
        return serviceId.equals("global");
    }

    public boolean isInstanceGlobal() {
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

/*
public ConfigProperty(String propertyKey, String value) {
        String[] propertyFields = propertyKey.split("\\.");
        this.parentProperty = propertyFields[0];
        if (!propertyFields[1].equals("global")) {
            this.serviceId = propertyFields[1];
            if (!propertyFields[2].equals("global")) {
                this.instanceId = propertyFields[2]+"@"+propertyFields[3];
                this.propertyElements = Arrays.copyOfRange(propertyFields, 4, propertyFields.length);
            } else {
                this.instanceId = null;
                this.propertyElements = Arrays.copyOfRange(propertyFields, 3, propertyFields.length);
            }
        } else {
            this.serviceId = null;
            this.instanceId = null;
            this.propertyElements = Arrays.copyOfRange(propertyFields, 2, propertyFields.length);
        }
        this.value = value;
    }

    public String getServiceId() {
        return isServiceGlobal() ? null : serviceId.toUpperCase();
    }

    public boolean isServiceGlobal() {
        return serviceId == null;
    }

    public boolean isInstanceGlobal() {
        return instanceId == null;
    }

    public String getParentProperty() {
        return parentProperty;
    }

    public String getInstanceId() {
        if (!isInstanceGlobal()) {
            return instanceId.split("@")[0]+"@"+getAddress();
        }
        return null;
    }

    public String getAddress() {
        if (!isInstanceGlobal()) {
            String addressField = instanceId.split("@")[1];
            int lastIndex = addressField.lastIndexOf("_");
            if (lastIndex == -1) { throw new RuntimeException("Invalid address field: " + addressField); }
            String host = addressField.substring(0, lastIndex).replace("_", ".");
            String port = addressField.substring(lastIndex + 1);
            return host + ":" + port;
        }
        return null;
    }
 */
