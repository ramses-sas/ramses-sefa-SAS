package it.polimi.ramses.loadbalancer;

import it.polimi.ramses.loadbalancer.algorithms.RandomLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.WeightedRandomLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;
import it.polimi.ramses.loadbalancer.algorithms.RoundRobinLoadBalancer;

public enum LoadBalancerType {
    ROUND_ROBIN,
    RANDOM,
    WEIGHTED_RANDOM,
    WEIGHTED_ROUND_ROBIN;
    //Custom

    public Class<? extends BaseLoadBalancer> getLoadBalancerClass() {
        //log.info("Test property: {}", common);
        return switch (this) {
            case ROUND_ROBIN -> RoundRobinLoadBalancer.class;
            case RANDOM -> RandomLoadBalancer.class;
            case WEIGHTED_RANDOM -> WeightedRandomLoadBalancer.class;
            case WEIGHTED_ROUND_ROBIN -> WeightedRoundRobinLoadBalancer.class;
        };
    }
}
