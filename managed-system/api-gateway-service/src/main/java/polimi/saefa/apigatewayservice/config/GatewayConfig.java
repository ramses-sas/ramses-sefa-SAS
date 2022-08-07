package polimi.saefa.apigatewayservice.config;

import com.netflix.discovery.EurekaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    @Autowired
    private EurekaClient discoveryClient;

    @Bean
    public GlobalFilter customFilter() {
        return new CustomGlobalFilter();
    }

    public class CustomGlobalFilter implements GlobalFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            logger.info("custom global filter");
            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return -1;
        }
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