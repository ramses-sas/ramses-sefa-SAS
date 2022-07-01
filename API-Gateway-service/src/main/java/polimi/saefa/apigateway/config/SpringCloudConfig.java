package polimi.saefa.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringCloudConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/client/**")
                        .uri("http://localhost:8081/"))
                .route(r -> r.path("/")
                        .uri("http://localhost:8081/"))
                .route(r -> r.path("/admin/**")
                        .uri("http://localhost:8081/"))

                .route(r -> r.path("/restaurants/**")
                        .uri("http://localhost:8081/"))
                .build();
    }

}