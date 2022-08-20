package it.polimi.saefa.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import it.polimi.saefa.loadbalancer.core.LoadBalancerFactory;
import it.polimi.saefa.loadbalancer.suppliers.InstanceListSupplierFactory;

@Slf4j
@Configuration
public class LoadBalancerConfig {

    @Autowired
    DiscoveryClient discoveryClient;

    @Bean
    @ConditionalOnMissingBean
    public InstanceListSupplierFactory instanceListSupplierFactory() {
        return new InstanceListSupplierFactory(discoveryClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerFactory loadBalancerFactory() {
        return new LoadBalancerFactory();
    }

}
