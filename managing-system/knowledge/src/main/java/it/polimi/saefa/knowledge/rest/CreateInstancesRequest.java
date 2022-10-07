package it.polimi.saefa.knowledge.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedList;
import java.util.List;

@Data
@NoArgsConstructor
public class CreateInstancesRequest {
    private String serviceId;
    private String serviceImplementationId;
    private List<String> instanceAddresses = new LinkedList<>();

    @JsonIgnore
    public List<String> getInstanceIds(){
        return instanceAddresses.stream().map(address -> serviceImplementationId + "@" + address).toList();
    }

    public CreateInstancesRequest(String serviceId, String serviceImplementationId) {
        this.serviceId = serviceId;
        this.serviceImplementationId = serviceImplementationId;
    }

    public void addInstanceAddress(String instanceAddress){
        instanceAddresses.add(instanceAddress);
    }
}
