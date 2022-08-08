package polimi.saefa.apigatewayservice.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.core.env.Environment;

public class CustomDiscSupplier extends DiscoveryClientServiceInstanceListSupplier {

    public CustomDiscSupplier(DiscoveryClient delegate, Environment environment) {
        super(delegate, environment);
    }

    public CustomDiscSupplier(ReactiveDiscoveryClient delegate, Environment environment) {
        super(delegate, environment);
    }


}
