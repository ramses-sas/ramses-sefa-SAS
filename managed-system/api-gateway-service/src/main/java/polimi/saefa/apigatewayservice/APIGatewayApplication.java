package polimi.saefa.apigatewayservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class APIGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(APIGatewayApplication.class, args);
    }


    // cosi posso scrivere custom logic per il refresh.
    @EventListener(EnvironmentChangeEvent.class)
    public void onApplicationEvent(EnvironmentChangeEvent environmentChangeEvent) {
        // Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
    }
}
