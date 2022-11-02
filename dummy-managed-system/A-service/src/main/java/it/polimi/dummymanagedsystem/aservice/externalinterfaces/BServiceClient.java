package it.polimi.dummymanagedsystem.aservice.externalinterfaces;

import it.polimi.dummymanagedsystem.aservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "B-SERVICE")
@LoadBalancerClient(name = "B-SERVICE", configuration = LoadBalancerConfig.class)
public interface BServiceClient {

    @GetMapping("/rest/generateRandomInt")
    int generateRandomInteger();

}

