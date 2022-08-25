package it.polimi.saefa.apigatewayservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@LoadBalancerClients({
    @LoadBalancerClient(name = "RESTAURANT-SERVICE", configuration = LoadBalancerConfig.class),
    @LoadBalancerClient(name = "ORDERING-SERVICE", configuration = LoadBalancerConfig.class)
})
public class GatewayConfig {

    @Bean
    @ConditionalOnMissingBean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String restaurantServiceUrl = "lb://RESTAURANT-SERVICE";
        String orderingServiceUrl = "lb://ORDERING-SERVICE";
        return builder.routes()
            .route(r -> r.path("/customer/cart/**")
                .filters(f -> f.rewritePath("/customer/cart", "/rest"))
                .uri(orderingServiceUrl))
            .route(r -> r.path("/customer/**")
                .filters(f -> f.prefixPath("/rest"))
                .uri(restaurantServiceUrl))
            .route(r -> r.path("/admin/**")
                .filters(f -> f.prefixPath("/rest"))
                .uri(restaurantServiceUrl))
            .route(r -> r.path("/test/**")
                .filters(f -> f.prefixPath("/rest"))
                .uri(restaurantServiceUrl))
            .build();
    }

}