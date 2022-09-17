package it.polimi.saefa.instancesmanager.restinterface;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface InstancesManagerRestInterface {

    @PostMapping(path = "/addInstances")
    AddInstancesResponse addInstances(@RequestBody AddInstancesRequest request);

    @PostMapping(path = "/removeInstance")
    RemoveInstanceResponse removeInstance(@RequestBody RemoveInstanceRequest request);
}
