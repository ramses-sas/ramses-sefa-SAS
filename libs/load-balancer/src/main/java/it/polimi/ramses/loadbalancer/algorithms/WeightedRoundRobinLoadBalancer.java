package it.polimi.ramses.loadbalancer.algorithms;

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

    // The weights of the services. Key is the instanceAddress, value is the weight.
    protected Map<String, Integer> weightPerAddress;

    private int defaultWeight;

    public WeightedRoundRobinLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider, int defaultWeight) {
        super(serviceInstanceListSupplierProvider);
        weightPerAddress = new HashMap<>();
        this.defaultWeight = defaultWeight;
    }

    public void setWeightForInstanceAtAddress(String instanceAddress, Integer weight) {
        if (weight == null || weight <= 0)
            weightPerAddress.remove(instanceAddress);
        else
            weightPerAddress.put(instanceAddress, weight);
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
                Integer instanceWeight = weightPerAddress.getOrDefault(instance.getHost()+":"+instance.getPort(), defaultWeight);
                for (int i = 0; i < instanceWeight; i++) {
                    weightedServiceInstances.add(instance);
                }
            }
            int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
            ServiceInstance instance = weightedServiceInstances.get(pos % weightedServiceInstances.size());
            serviceInstanceResponse = new DefaultResponse(instance);
            if (log.isDebugEnabled()) {
                log.debug("WeightedRoundRobinLoadBalancer: selected instance "+instance.getInstanceId()+" with weight "+ weightPerAddress.get(instance.getHost()+":"+instance.getPort()));
            }
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
