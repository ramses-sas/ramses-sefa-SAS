package polimi.saefa.loadbalancer.algorithms;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WeightedRoundRobinLoadBalancer extends RoundRobinLoadBalancer {

    // The weights of the services. Key is the instanceId, value is the weight.
    protected Map<String,Integer> instancesWeights;

    private int defaultWeight = 1;

    public WeightedRoundRobinLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider) {
        super(serviceInstanceListSupplierProvider);
        instancesWeights = new HashMap<>();
    }

    public void setWeight(String instanceId, int weight) {
        instancesWeights.put(instanceId, weight);
    }

    @Override
    protected Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> serviceInstances) {
        Response<ServiceInstance> serviceInstanceResponse;
        if (serviceInstances.isEmpty()) {
            if (log.isWarnEnabled())
                log.warn("No servers available for service: " + getServiceId());
            serviceInstanceResponse = new EmptyResponse();
        } else {
            List<ServiceInstance> weightedServiceInstances = new ArrayList<>();
            for (ServiceInstance instance : serviceInstances) {
                Integer instanceWeight = instancesWeights.getOrDefault(instance.getInstanceId(), 1);
                for (int i = 0; i < instanceWeight; i++) {
                    weightedServiceInstances.add(instance);
                }
            }
            int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
            ServiceInstance instance = weightedServiceInstances.get(pos % weightedServiceInstances.size());
            serviceInstanceResponse = new DefaultResponse(instance);
        }
        return serviceInstanceResponse;
    }

    public int getDefaultWeight() {
        return defaultWeight;
    }

    public void setDefaultWeight(int defaultWeight) {
        this.defaultWeight = defaultWeight;
    }

}
