package polimi.saefa.apigatewayservice.config;

import lombok.Getter;
import lombok.Setter;
//import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.RoundRobinLoadBalancer;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;

public class LoadBalancerConfiguration /*extends LoadBalancerProperties*/ {
    enum LoadBalancerType {
        RoundRobin,
        WeightedRoundRobin
        //Custom
    }

    @Getter @Setter
    private LoadBalancerType loadBalancerType;

    public LoadBalancerConfiguration() {
        //super();
        this.loadBalancerType = LoadBalancerType.RoundRobin;
    }

    public LoadBalancerConfiguration(LoadBalancerType loadBalancerType) {
        //super();
        this.loadBalancerType = loadBalancerType;
    }

    public Class getLoadBalancerClass() {
        return switch (loadBalancerType) {
            case RoundRobin -> RoundRobinLoadBalancer.class;
            //case Custom -> RoundRobinLoadBalancer.class;
            case WeightedRoundRobin -> WeightedRoundRobinLoadBalancer.class;
        };
    }


}
