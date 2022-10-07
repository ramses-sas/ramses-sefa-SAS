package it.polimi.saefa.execute.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "INSTANCES-MANAGER", url = "${INSTANCES_MANAGER_ACTUATOR_URL}")
public interface InstancesManagerClient {

    @PostMapping(path = "/rest/addInstances")
    StartNewInstancesResponse addInstances(@RequestBody StartNewInstancesRequest request);

    @PostMapping(path = "/rest/removeInstance")
    RemoveInstanceResponse removeInstance(@RequestBody RemoveInstanceRequest request);
}
