package it.polimi.dummymanagedsystem.randintvendorservice.externalinterfaces;

import it.polimi.dummymanagedsystem.randintvendorservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "RANDINT-PRODUCER-SERVICE")
@LoadBalancerClient(name = "RANDINT-PRODUCER-SERVICE", configuration = LoadBalancerConfig.class)
public interface RandintProducerServiceClient {

    @GetMapping("/rest/generateRandomInt")
    int generateRandomInteger();

}

