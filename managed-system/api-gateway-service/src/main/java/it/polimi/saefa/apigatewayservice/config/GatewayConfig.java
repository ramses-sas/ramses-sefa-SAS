package it.polimi.saefa.apigatewayservice.config;

import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@LoadBalancerClients({
    @LoadBalancerClient(name = "RESTAURANT-SERVICE", configuration = LoadBalancerConfig.class),
    @LoadBalancerClient(name = "ORDERING-SERVICE", configuration = LoadBalancerConfig.class)
})
public class GatewayConfig {

    /*
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String restaurantServiceUrl = "lb://RESTAURANT-SERVICE";
        String orderingServiceUrl = "lb://ORDERING-SERVICE";


        log.warn("Allocating route locator");

        return builder.routes()
            .route(r -> r.path("/customer/cart/**")
                .filters(f -> f.rewritePath("/customer/cart", "/rest")
                        .retry(retryConfig -> {
                            retryConfig.setRouteId("route1");
                            retryConfig.setRetries(5).setMethods(HttpMethod.GET, HttpMethod.POST);
                        }))
                .uri(orderingServiceUrl))


            .route(r -> r.path("/customer/**")
                .filters(f -> f.prefixPath("/rest")
                        .retry(retryConfig -> {
                            retryConfig.setRouteId("route2");
                            retryConfig.setRetries(30).setMethods(HttpMethod.GET, HttpMethod.POST);
                        }))
                .uri(restaurantServiceUrl))


            .route(r -> r.path("/admin/**")
                .filters(f -> f.prefixPath("/rest")
                        .retry(retryConfig -> {
                            retryConfig.setRouteId("route3");
                            retryConfig.setRetries(30).setMethods(HttpMethod.GET, HttpMethod.POST);
                        }))
                .uri(restaurantServiceUrl))
            .route(r -> r.path("/test/**")
                .filters(f -> f.prefixPath("/rest")
                        .retry(retryConfig -> {
                            retryConfig.setRouteId("route4");
                            retryConfig.setRetries(30).setMethods(HttpMethod.GET, HttpMethod.POST);
                        }))
                .uri(restaurantServiceUrl))
            .build();
    }

     */
}