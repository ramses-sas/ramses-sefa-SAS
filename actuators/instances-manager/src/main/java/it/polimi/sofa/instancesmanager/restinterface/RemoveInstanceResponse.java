package it.polimi.sofa.instancesmanager.restinterface;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveInstanceResponse {
    private String serviceImplementationName;
    private String address;
    private int port;
}
