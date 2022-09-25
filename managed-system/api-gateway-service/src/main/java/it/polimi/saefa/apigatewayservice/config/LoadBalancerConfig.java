package it.polimi.saefa.apigatewayservice.config;

import it.polimi.saefa.configparser.CustomPropertiesReader;
import it.polimi.saefa.loadbalancer.LoadBalancerType;
import it.polimi.saefa.loadbalancer.algorithms.RandomLoadBalancer;
import it.polimi.saefa.loadbalancer.algorithms.RoundRobinLoadBalancer;
import it.polimi.saefa.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
public class LoadBalancerConfig {

    @Bean
    public ReactorLoadBalancer<ServiceInstance> customLoadBalancer(
        ServiceInstanceListSupplier supplier, CustomPropertiesReader<Environment> configParser
    ) {
        String serviceId = supplier.getServiceId();
        LoadBalancerType type = LoadBalancerType.valueOf(configParser.getLoadBalancerTypeOrDefault(serviceId));
        log.debug("LoadBalancerClient for {}: creating load balancer of type {}", serviceId, type);
        switch (type) {
            case RANDOM:
                return new RandomLoadBalancer(supplier);
            case ROUND_ROBIN:
                return new RoundRobinLoadBalancer(supplier);
            case WEIGHTED_ROUND_ROBIN:
                // Se il load balancer è di tipo weighted leggi i pesi dal config
                int defaultWeight = configParser.getLoadBalancerServiceDefaultWeight(serviceId);
                WeightedRoundRobinLoadBalancer wlb = new WeightedRoundRobinLoadBalancer(supplier, defaultWeight);
                List<ServiceInstance> instances = supplier.get().blockFirst();
                if (instances != null) {
                    int instanceWeight;
                    String address;
                    for (ServiceInstance instance : instances) {
                        address = instance.getHost() + ":" + instance.getPort();
                        instanceWeight = configParser.getLoadBalancerInstanceWeight(serviceId, address);
                        wlb.setWeightForInstanceAtAddress(address, instanceWeight);
                        log.debug("LoadBalancerClient for service {} at {}: setting weight to {}", serviceId, address, instanceWeight);
                    }
                }
                return wlb;
            default:
                throw new IllegalArgumentException("Unknown load balancer type: " + type);
        }
    }

}
