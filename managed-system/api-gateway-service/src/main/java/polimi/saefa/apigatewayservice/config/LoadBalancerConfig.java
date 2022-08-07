package polimi.saefa.apigatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplierBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class LoadBalancerConfig {

    @Autowired
    Environment environment;


    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        return new CustomServiceInstanceListSupplier(context);
                //.withDiscoveryClient()
                //.withZonePreference()
                //.withCaching()
                //.build(context);
    }


}

class CustomServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    final Logger logger = LoggerFactory.getLogger(CustomServiceInstanceListSupplier.class);

    CustomServiceInstanceListSupplier(ConfigurableApplicationContext env) {
        this.serviceId = env.getApplicationName();
        logger.warn("serviceId: {}", serviceId);
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return Flux.just(Arrays
                .asList(new DefaultServiceInstance(serviceId + "1", serviceId, "localhost", 8090, false),
                new DefaultServiceInstance(serviceId + "2", serviceId, "localhost", 9092, false),
                new DefaultServiceInstance(serviceId + "3", serviceId, "localhost", 9999, false)));
    }
}
