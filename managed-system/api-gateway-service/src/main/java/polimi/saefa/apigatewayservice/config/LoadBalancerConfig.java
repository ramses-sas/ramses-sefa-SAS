package polimi.saefa.apigatewayservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoadBalancerConfig {
    /*@Bean
    public ServiceInstanceListSupplier customServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        Logger.getLogger(LoadBalancerConfig.class.getName()).warning("GOING INTO CUSTOM SERVICE INSTANCE LIST SUPPLIER");
        DiscoveryClient discoveryClient = context.getBean(DiscoveryClient.class);
        return new CustomServiceInstanceListSupplier(context);
        //return new CustomDiscSupplier(discoveryClient, context.getEnvironment(), "RESTAURANT-SERVICE");
    }*/

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ConfigurableApplicationContext context;

    /*
    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties) {
        LoadBalancerClientFactory factory = new LoadBalancerClientFactory(properties);
        logger.warn("LOAD BALANCER CLIENT FACTORY");
        return factory;

    }
*/
    /*
    @Bean
    public RoundRobinLoadBalancer loadBalancer1(Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {
        return new RoundRobinLoadBalancer(context.getBean(), "RESTAURANT-SERVICE");
    }

    @Bean
    public RoundRobinLoadBalancer loadBalancer2(Environment environment, LoadBalancerClientFactory loadBalancerClientFactory) {
        return new RoundRobinLoadBalancer(loadBalancerClientFactory.getLazyProvider("ORDERING-SERVICE", ServiceInstanceListSupplier.class), "ORDERING-SERVICE");
    }*/
}

