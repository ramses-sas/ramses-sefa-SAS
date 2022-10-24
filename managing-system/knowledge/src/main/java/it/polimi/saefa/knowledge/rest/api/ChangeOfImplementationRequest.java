package it.polimi.saefa.knowledge.rest.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChangeOfImplementationRequest {
    private String serviceId;
    private String newImplementationId;
    private List<String> newInstancesAddresses;
}
