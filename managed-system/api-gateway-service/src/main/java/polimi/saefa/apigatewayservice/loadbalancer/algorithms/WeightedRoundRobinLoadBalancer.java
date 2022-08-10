package polimi.saefa.apigatewayservice.loadbalancer.algorithms;

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

    protected Map<String,Integer> instancesWeight;

    public WeightedRoundRobinLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider) {
        super(serviceInstanceListSupplierProvider);
        instancesWeight = new HashMap<>();
    }

    public void updateWeight(String serviceId, int weight) {
        instancesWeight.put(serviceId, weight);
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
                Integer instanceWeight = instancesWeight.getOrDefault(instance.getServiceId(), 2);
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



}
