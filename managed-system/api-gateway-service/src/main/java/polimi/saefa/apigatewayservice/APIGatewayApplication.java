package polimi.saefa.apigatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import polimi.saefa.apigatewayservice.config.LoadBalancerConfig;


@EnableDiscoveryClient
@LoadBalancerClient(name = "APIGatewayApplication", configuration = LoadBalancerConfig.class)
@SpringBootApplication
public class APIGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(APIGatewayApplication.class, args);
    }

}
