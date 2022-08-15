package polimi.saefa.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import polimi.saefa.configparser.ConfigParser;
import polimi.saefa.configparser.ConfigProperty;
import polimi.saefa.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
import polimi.saefa.loadbalancer.core.BaseLoadBalancer;
import polimi.saefa.loadbalancer.core.LoadBalancerFactory;
import polimi.saefa.loadbalancer.core.LoadBalancerType;
import polimi.saefa.loadbalancer.suppliers.InstanceListSupplierFactory;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class ConfigParserConfig {

    @Autowired
    Environment env;

    @Autowired
    InstanceListSupplierFactory instanceListSupplierFactory;

    @Autowired
    LoadBalancerFactory loadBalancerFactory;

    @Bean
    @ConditionalOnMissingBean
    public ConfigParser configParser() {
        return new ConfigParser(env);
    }

    @EventListener(value = EnvironmentChangeEvent.class)
    public void register(EnvironmentChangeEvent environmentChangeEvent) {
        // EXAMPLE: Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
        List<String> lbKeys = environmentChangeEvent.getKeys().stream()
                .map(key -> key.startsWith("loadbalancing.") ? key : null)
                .filter(Objects::nonNull).distinct().toList();
        lbKeys.forEach(key -> {
            log.info("Parsing key: " + key);
            ConfigProperty prop = configParser().parse(key);
            handleLBWeightChange(prop);
            handleLBTypeChange(prop);
        });
    }

    private void handleLBTypeChange(ConfigProperty changedProperty) {
        if (changedProperty.getPropertyElements().length == 1 && changedProperty.getPropertyElements()[0].equals("type")) {
            ServiceInstanceListSupplier supplier = instanceListSupplierFactory.createEurekaSupplierIfNeeded(changedProperty.getServiceId());
            switch (changedProperty.getValue()) {
                case "ROUND_ROBIN" -> loadBalancerFactory.createIfNeeded(LoadBalancerType.ROUND_ROBIN, supplier);
                case "WEIGHTED_ROUND_ROBIN" -> loadBalancerFactory.createIfNeeded(LoadBalancerType.WEIGHTED_ROUND_ROBIN, supplier);
                default -> throw new RuntimeException("Unknown load balancer type: " + changedProperty.getValue());
            }
        }
    }

    private void handleLBWeightChange(ConfigProperty changedProperty) {
        if (!changedProperty.isGlobal()) {
            if (changedProperty.getPropertyElements().length == 1 && changedProperty.getPropertyElements()[0].equals("weight")) {
                int weight = Integer.parseInt(changedProperty.getValue());
                BaseLoadBalancer lb = loadBalancerFactory.getLoadBalancers().get(changedProperty.getServiceId());
                if (lb instanceof WeightedRoundRobinLoadBalancer) {
                    log.info("Changing load balancer weight for service {} to {}", changedProperty.getServiceId(), changedProperty.getValue());
                    ((WeightedRoundRobinLoadBalancer) lb).setWeight(changedProperty.getInstanceId(), weight);
                }
            }
        }
    }


}
