package it.polimi.saefa.apigatewayservice.config;

import com.netflix.discovery.DiscoveryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import it.polimi.saefa.apigatewayservice.ServedServices;
import it.polimi.saefa.apigatewayservice.filters.LoadBalancerFilter;
import it.polimi.saefa.configparser.ConfigParser;
import it.polimi.saefa.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
import it.polimi.saefa.loadbalancer.core.BaseLoadBalancer;
import it.polimi.saefa.loadbalancer.core.LoadBalancerFactory;
import it.polimi.saefa.loadbalancer.core.LoadBalancerType;
import it.polimi.saefa.loadbalancer.suppliers.InstanceListSupplierFactory;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
@Configuration
@AutoConfigureAfter(DiscoveryClient.class)
public class GatewayConfig {

    @Autowired
    LoadBalancerFactory lbFactory;

    @Autowired
    InstanceListSupplierFactory supplierFactory;

    @Autowired
    ConfigParser<Environment> configParser;


    @Bean
    @ConditionalOnMissingBean
    public GlobalFilter loadBalancerFilter() {
        // Per ogni servizio di cui fare load balancing
        for (ServedServices service : ServedServices.values()) {
            String serviceId = service.getServiceId();
            // Crea un load balancer del tipo specificato nel config (RoundRobin di default)
            LoadBalancerType loadBalancerType = LoadBalancerType.valueOf(configParser.getLBType(serviceId));
            log.info("LoadBalancerFilter: configuring a "+loadBalancerType+" load balancer for service "+serviceId);
            ServiceInstanceListSupplier supplier = supplierFactory.createEurekaSupplier(serviceId);
            BaseLoadBalancer lb = lbFactory.create(loadBalancerType, supplier);

            // Se il load balancer è di tipo weighted leggi i pesi dal config
            if (loadBalancerType == LoadBalancerType.WEIGHTED_ROUND_ROBIN) {
                int defaultWeight = configParser.getLBWeight(serviceId);
                WeightedRoundRobinLoadBalancer wlb = (WeightedRoundRobinLoadBalancer) lb;
                wlb.setDefaultWeight(defaultWeight);
                List<ServiceInstance> instances = supplier.get().blockFirst();
                if (instances != null) {
                    int instanceWeight;
                    for (ServiceInstance instance : instances) {
                        instanceWeight = configParser.getLBWeight(serviceId, instance.getInstanceId());
                        wlb.setWeight(instance.getInstanceId(), instanceWeight);
                        log.info("LoadBalancerFilter: setting weight for "+instance.getInstanceId()+" to "+instanceWeight);
                    }
                }
            }
            log.info("LoadBalancerFilter: "+lb.getClass().getSimpleName()+" correctly configured for "+serviceId);
        }
        return new LoadBalancerFilter(lbFactory);
    }

    @Bean
    @ConditionalOnMissingBean
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


}