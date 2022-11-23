package it.polimi.dummymanagedsystem.randintvendorservice.config;

import it.polimi.ramses.configparser.CustomPropertiesReader;
import it.polimi.ramses.configparser.CustomProperty;
import it.polimi.ramses.loadbalancer.algorithms.WeightedRandomLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class CustomPropertiesHandler {

    @Autowired
    Environment env;

    @Autowired
    LoadBalancerClientFactory loadBalancerClientFactory;

    @Bean
    @ConditionalOnMissingBean
    public CustomPropertiesReader<Environment> customPropertiesReader() {
        return new CustomPropertiesReader<>(env);
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void lbChanges(EnvironmentChangeEvent environmentChangeEvent) {
        // EXAMPLE: Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
        List<String> lbKeys = environmentChangeEvent.getKeys().stream()
                .map(key -> key.startsWith("loadbalancing.") ? key : null).filter(Objects::nonNull).toList();
        lbKeys.forEach(key -> {
            log.info("Parsing key: " + key);
            CustomProperty changedProperty = customPropertiesReader().parse(key);
            handleLBTypeChange(changedProperty);
            handleLBWeightChange(changedProperty);
        });
    }


    private void handleLBTypeChange(CustomProperty changedProperty) {
        if (changedProperty.getPropertyElements().length == 1 && changedProperty.getPropertyElements()[0].equals("type")) {
            loadBalancerClientFactory.destroy();
        }
    }

    private void handleLBWeightChange(CustomProperty changedProperty) {
        if (!changedProperty.isServiceGlobal() && !changedProperty.isInstanceGlobal()) {
            if (changedProperty.getPropertyElements().length == 1 && changedProperty.getPropertyElements()[0].equals("weight")) {
                String stringWeight = changedProperty.getValue();
                ReactiveLoadBalancer<ServiceInstance> lb = loadBalancerClientFactory.getInstance(changedProperty.getServiceId());
                if (lb instanceof WeightedRoundRobinLoadBalancer) {
                    log.info("Changing load balancer weight for instance {} of service {} to {}", changedProperty.getServiceId(), changedProperty.getAddress(), changedProperty.getValue());
                    Integer weight = null;
                    try {
                        weight = Integer.parseInt(stringWeight);
                    } catch (Exception e) {}
                    ((WeightedRoundRobinLoadBalancer) lb).setWeightForInstanceAtAddress(changedProperty.getAddress(), weight);
                } else if (lb instanceof WeightedRandomLoadBalancer) {
                    log.info("Changing load balancer weight for instance {} of service {} to {}", changedProperty.getServiceId(), changedProperty.getAddress(), changedProperty.getValue());
                    Double weight = null;
                    try {
                        weight = Double.parseDouble(stringWeight);
                    } catch (Exception e) {}
                    ((WeightedRandomLoadBalancer) lb).setWeightForInstanceAtAddress(changedProperty.getAddress(), weight);
                }
            }
        }
    }

}
