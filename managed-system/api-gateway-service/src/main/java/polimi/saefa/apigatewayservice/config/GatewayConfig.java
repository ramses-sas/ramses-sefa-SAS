package polimi.saefa.apigatewayservice.config;

import com.netflix.discovery.EurekaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import polimi.saefa.apigatewayservice.filters.LoadBalancerFilter;


@Configuration
public class GatewayConfig {
    final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    @Autowired
    private EurekaClient discoveryClient;

    @Autowired
    ConfigurableApplicationContext context;

    @Bean
    public GlobalFilter loadBalancerFilter() {
        return new LoadBalancerFilter(context);
    }

    @Bean
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