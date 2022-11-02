package it.polimi.saefa.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@LoadBalancerClients({
    @LoadBalancerClient(name = "A-SERVICE", configuration = LoadBalancerConfig.class)
})
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String aServiceUrl = "lb://A-SERVICE";


        log.warn("Allocating route locator");

        return builder.routes()
            .route(r -> r.path("/**")
                .filters(f -> f.prefixPath("/rest"))
                .uri(aServiceUrl))
            .build();
    }
}