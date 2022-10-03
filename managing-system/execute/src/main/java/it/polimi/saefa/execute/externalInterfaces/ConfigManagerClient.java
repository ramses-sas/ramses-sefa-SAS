package it.polimi.saefa.execute.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "CONFIG-MANAGER", url = "${CONFIG_MANAGER_ACTUATOR_URL}")
public interface ConfigManagerClient {

    @PostMapping(path = "/rest/changeProperty")
    void changeProperty(@RequestBody ChangePropertyRequest request);

}
