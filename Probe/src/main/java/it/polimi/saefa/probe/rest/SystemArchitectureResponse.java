package it.polimi.saefa.probe.rest;

import it.polimi.saefa.probe.domain.Service;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SystemArchitectureResponse {
    List<Service> services;

    public SystemArchitectureResponse(List<Service> services) {
        this.services = services;
    }
}
