package it.polimi.saefa.execute.externalInterfaces;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddOrUpdatePropertyRequest {
    private String serviceName;
    private String propertyName;
    private String value;
}
