package it.polimi.saefa.knowledge.rest.api;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class AddInstanceRequest {
    private String serviceId;
    private String newInstanceAddress;

    public AddInstanceRequest(String serviceId, String newInstanceAddress) {
        this.serviceId = serviceId;
        this.newInstanceAddress = newInstanceAddress;
    }
}
