package polimi.saefa.apigatewayservice.config;

import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringCloudConfig {

    @Autowired
    private EurekaClient discoveryClient;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String restaurantServiceUrl = "lb://RESTAURANT-SERVICE";
        String webServiceUrl = "lb://WEB-SERVICE";
        return builder.routes()
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