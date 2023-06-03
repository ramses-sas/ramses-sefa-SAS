package it.polimi.sefa.apigatewayservice.config;

import it.polimi.ramses.configparser.CustomPropertiesReader;
import it.polimi.ramses.loadbalancer.LoadBalancerType;
import it.polimi.ramses.loadbalancer.algorithms.RandomLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.RoundRobinLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.WeightedRandomLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
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
        List<ServiceInstance> instances = supplier.get().blockFirst();
        switch (type) {
            case RANDOM:
                return new RandomLoadBalancer(supplier);
            case ROUND_ROBIN:
                return new RoundRobinLoadBalancer(supplier);
            case WEIGHTED_ROUND_ROBIN:
                WeightedRoundRobinLoadBalancer wlb = new WeightedRoundRobinLoadBalancer(supplier, 1);
                if (instances != null) {
                    String instanceWeight;
                    String address;
                    for (ServiceInstance instance : instances) {
                        address = instance.getHost() + ":" + instance.getPort();
                        instanceWeight = configParser.getLoadBalancerInstanceWeight(serviceId, address);
                        if (instanceWeight != null) {
                            wlb.setWeightForInstanceAtAddress(address, Integer.parseInt(instanceWeight));
                            log.debug("WeightedRoundRobinLoadBalancer for service {} at {}: setting weight to {}", serviceId, address, instanceWeight);
                        }
                    }
                }
                return wlb;
            case WEIGHTED_RANDOM:
                WeightedRandomLoadBalancer wrlb = new WeightedRandomLoadBalancer(supplier);
                if (instances != null) {
                    String instanceWeight;
                    String address;
                    for (ServiceInstance instance : instances) {
                        address = instance.getHost() + ":" + instance.getPort();
                        instanceWeight = configParser.getLoadBalancerInstanceWeight(serviceId, address);
                        if (instanceWeight != null) {
                            wrlb.setWeightForInstanceAtAddress(address, Double.parseDouble(instanceWeight));
                            log.debug("WeightedRandomLoadBalancer for service {} at {}: setting weight to {}", serviceId, address, instanceWeight);
                        }
                    }
                }
                return wrlb;
            default:
                throw new IllegalArgumentException("Unknown load balancer type: " + type);
        }
    }
}
