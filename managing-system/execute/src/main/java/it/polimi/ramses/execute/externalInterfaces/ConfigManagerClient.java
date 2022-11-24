package it.polimi.ramses.execute.externalInterfaces;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "CONFIG-MANAGER", url = "${CONFIG_MANAGER_ACTUATOR_URL}")
@Retry(name = "CONFIG-MANAGER")
public interface ConfigManagerClient {

    @PostMapping(path = "/rest/changeProperty")
    void changeProperty(@RequestBody ChangePropertyRequest request);

    @PostMapping(path = "/rest/changeLBWeights")
    void changeLBWeights(@RequestBody ChangeLBWeightsRequest request);

}
