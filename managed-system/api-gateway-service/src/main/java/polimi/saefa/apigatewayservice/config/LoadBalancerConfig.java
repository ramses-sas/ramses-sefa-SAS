package polimi.saefa.apigatewayservice.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

//@Configuration
// NOT USED
public class LoadBalancerConfig {
    @Bean
    public ServiceInstanceListSupplier customServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        Logger.getLogger(LoadBalancerConfig.class.getName()).warning("GOING INTO CUSTOM SERVICE INSTANCE LIST SUPPLIER");
        DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
        return new CustomServiceInstanceListSupplier(context);
        //return new CustomDiscSupplier(discoveryClient, context.getEnvironment(), "RESTAURANT-SERVICE");
    }
}

