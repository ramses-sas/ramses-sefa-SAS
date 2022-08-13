package polimi.saefa.apigatewayservice.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import polimi.saefa.apigatewayservice.loadbalancer.LoadBalancerType;

@Slf4j
//@Configuration
public class LoadBalancerConfiguration {

    @Getter @Setter
    private LoadBalancerType loadBalancerType;


    public LoadBalancerConfiguration() {
        //super();
        this.loadBalancerType = LoadBalancerType.ROUND_ROBIN;
    }

    public LoadBalancerConfiguration(LoadBalancerType loadBalancerType) {
        //super();
        this.loadBalancerType = loadBalancerType;
    }

}
