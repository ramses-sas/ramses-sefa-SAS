package it.polimi.saefa.apigatewayservice.config;

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
import it.polimi.saefa.configparser.ConfigParser;
import it.polimi.saefa.configparser.ConfigProperty;
import it.polimi.saefa.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class ConfigParserConfig {

    @Autowired
    Environment env;

    @Autowired
    LoadBalancerClientFactory loadBalancerClientFactory;

    @Bean
    @ConditionalOnMissingBean
    public ConfigParser<Environment> configParser() {
        return new ConfigParser<>(env);
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void lbChanges(EnvironmentChangeEvent environmentChangeEvent) {
        // EXAMPLE: Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
        List<String> lbKeys = environmentChangeEvent.getKeys().stream()
                .map(key -> key.startsWith("loadbalancing.") ? key : null).filter(Objects::nonNull).toList();
        lbKeys.forEach(key -> {
            log.info("Parsing key: " + key);
            ConfigProperty changedProperty = configParser().parse(key);
            handleLBTypeChange(changedProperty);
            handleLBWeightChange(changedProperty);
        });
    }


    private void handleLBTypeChange(ConfigProperty changedProperty) {
        if (changedProperty.getPropertyElements().length == 1 && changedProperty.getPropertyElements()[0].equals("type")) {
            loadBalancerClientFactory.destroy();
        }
    }

    private void handleLBWeightChange(ConfigProperty changedProperty) {
        if (!changedProperty.isGlobal()) {
            if (changedProperty.getPropertyElements().length == 1 && changedProperty.getPropertyElements()[0].equals("weight")) {
                int weight = Integer.parseInt(changedProperty.getValue());
                ReactiveLoadBalancer<ServiceInstance> lb = loadBalancerClientFactory.getInstance(changedProperty.getServiceId());
                if (lb instanceof WeightedRoundRobinLoadBalancer) {
                    log.info("Changing load balancer weight for service {}@{} to {}", changedProperty.getServiceId(), changedProperty.getInstanceId(), changedProperty.getValue());
                    ((WeightedRoundRobinLoadBalancer) lb).setWeight(changedProperty.getInstanceId(), weight);
                }
            }
        }
    }

}
