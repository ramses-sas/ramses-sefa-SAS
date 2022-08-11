package polimi.saefa.apigatewayservice.config;

import lombok.Getter;
import lombok.Setter;
//import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import polimi.saefa.apigatewayservice.loadbalancer.BaseLoadBalancer;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.RoundRobinLoadBalancer;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;

@Slf4j
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

    public Class<? extends BaseLoadBalancer> getLoadBalancerClass() {
        //log.info("Test property: {}", common);
        return switch (loadBalancerType) {
            case RoundRobin -> RoundRobinLoadBalancer.class;
            //case Custom -> RoundRobinLoadBalancer.class;
            case WeightedRoundRobin -> WeightedRoundRobinLoadBalancer.class;
        };
    }


}
