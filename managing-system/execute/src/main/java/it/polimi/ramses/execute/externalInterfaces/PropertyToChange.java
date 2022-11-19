package it.polimi.ramses.execute.externalInterfaces;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PropertyToChange {
    private String serviceName;
    private String propertyName;
    private String value;

    // To add a property
    public PropertyToChange(String serviceName, String propertyName, String value) {
        this.serviceName = serviceName;
        this.propertyName = propertyName;
        this.value = value;
    }

    // To remove a property (the value is null)
    public PropertyToChange(String serviceName, String propertyName) {
        this.serviceName = serviceName;
        this.propertyName = propertyName;
        value = null;
    }
}
