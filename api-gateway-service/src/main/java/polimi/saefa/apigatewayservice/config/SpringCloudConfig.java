package polimi.saefa.apigatewayservice.config;

import com.netflix.appinfo.InstanceInfo;
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
        return builder.routes()
                .route(r -> r.path("/client/**")
                        .uri(restaurantServiceUrl))
                .route(r -> r.path("/")
                        .uri(restaurantServiceUrl))
                .route(r -> r.path("/admin/**")
                        .uri(restaurantServiceUrl))
                .route(r -> r.path("/restaurants/**")
                        .uri(restaurantServiceUrl))
                .build();
    }

}