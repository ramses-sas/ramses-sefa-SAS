package polimi.saefa.apigatewayservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;

public class LoadBalancerConfiguration extends LoadBalancerProperties {
    enum LoadBalancerType {
        RoundRobin,
        Random,
        Custom
    }

    @Getter @Setter
    private LoadBalancerType loadBalancerType;

    public LoadBalancerConfiguration() {
        super();
        this.loadBalancerType = LoadBalancerType.RoundRobin;
    }

    public LoadBalancerConfiguration(LoadBalancerType loadBalancerType) {
        super();
        this.loadBalancerType = loadBalancerType;
    }

    public Class getLoadBalancerClass() {
        return switch (loadBalancerType) {
            case Custom -> RandomLoadBalancer.class;
            case Random -> RandomLoadBalancer.class;
            default -> RoundRobinLoadBalancer.class;
        };
    }


}
