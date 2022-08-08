package polimi.saefa.apigatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import polimi.saefa.apigatewayservice.config.LoadBalancerConfig;

import java.util.Collections;


@EnableDiscoveryClient
@SpringBootApplication
//@LoadBalancerClient(name = "APIGatewayApplication")
public class APIGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(APIGatewayApplication.class, args);
    }

    @ConditionalOnMissingBean
    @Bean
    public LoadBalancerClientFactory loadBalancerClientFactory(LoadBalancerClientsProperties properties) {
        LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory(properties);
        return clientFactory;
    }
}
