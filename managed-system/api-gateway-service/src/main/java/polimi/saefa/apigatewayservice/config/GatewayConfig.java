package polimi.saefa.apigatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import polimi.saefa.apigatewayservice.loadbalancer.EurekaInstanceListSupplier;
import polimi.saefa.apigatewayservice.loadbalancer.LoadBalancerFactory;
import polimi.saefa.apigatewayservice.loadbalancer.LoadBalancerFilter;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.RoundRobinLoadBalancer;


@Configuration
public class GatewayConfig {
    final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);

    @Autowired
    DiscoveryClient discoveryClient;

    @Autowired
    LoadBalancerFactory factory;

    @Autowired
    ConfigurableApplicationContext context;


    @Bean
    public GlobalFilter loadBalancerFilter() {
        for (ServedServices service : ServedServices.values()) {
            logger.info("LoadBalancing: creating load balancer for {}", service.getServiceId());
            factory.register(new RoundRobinLoadBalancer(new EurekaInstanceListSupplier(discoveryClient, context.getEnvironment(), service.getServiceId())));
        }
        return new LoadBalancerFilter(factory);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        String restaurantServiceUrl = ServedServices.RESTAURANT_SERVICE.toLoadBalancerUri();
        String orderingServiceUrl = ServedServices.ORDERING_SERVICE.toLoadBalancerUri();
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

    enum ServedServices {
        // Un enum case per servizio che necessita di load balancing da parte del gateway
        // Il nome dell'enum deve essere uguale al nome del servizio, con un _ invece di un -
        RESTAURANT_SERVICE,
        ORDERING_SERVICE;

        public String getServiceId() { return this.name().replace("_", "-"); }
        public String toLoadBalancerUri() {
            return "lb://" + this.getServiceId();
        }
    }

}