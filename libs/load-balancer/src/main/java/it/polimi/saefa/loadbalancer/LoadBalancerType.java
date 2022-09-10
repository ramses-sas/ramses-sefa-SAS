package it.polimi.saefa.loadbalancer;

import it.polimi.saefa.loadbalancer.algorithms.*;

public enum LoadBalancerType {
    ROUND_ROBIN,
    RANDOM,
    WEIGHTED_ROUND_ROBIN;
    //Custom

    public Class<? extends BaseLoadBalancer> getLoadBalancerClass() {
        //log.info("Test property: {}", common);
        return switch (this) {
            case ROUND_ROBIN -> RoundRobinLoadBalancer.class;
            case RANDOM -> RandomLoadBalancer.class;
            case WEIGHTED_ROUND_ROBIN -> WeightedRoundRobinLoadBalancer.class;
        };
    }
}
