package it.polimi.saefa.instancesmanager.restinterface;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface InstancesManagerRestInterface {

    @PostMapping(path = "/addInstances")
    AddInstancesResponse deliverOrder(@RequestBody AddInstancesRequest request);


}
