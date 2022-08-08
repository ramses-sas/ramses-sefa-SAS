package polimi.saefa.apigatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import polimi.saefa.apigatewayservice.APIGatewayApplication;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

class CustomServiceInstanceListSupplier implements ServiceInstanceListSupplier {

    private final String serviceId;
    private final LoadBalancerProperties properties;
    final Logger logger = LoggerFactory.getLogger(APIGatewayApplication.class);

    CustomServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        this.serviceId = "AAAAAA";
        LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
        properties = loadBalancerClientFactory.getProperties(getServiceId());
        logger.warn(properties.toString());
        logger.warn("serviceId: {}", serviceId);
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        logger.warn("GOING INTO GET");
        return Flux.just(Arrays
                .asList(new DefaultServiceInstance(serviceId + "1", serviceId, "localhost", 8090, false),
                        new DefaultServiceInstance(serviceId + "2", serviceId, "localhost", 9092, false),
                        new DefaultServiceInstance(serviceId + "3", serviceId, "localhost", 9999, false)));
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        logger.warn("GOING INTO GET WITHOUT REQUEST");
        return null;
    }
}