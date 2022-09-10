package it.polimi.saefa.loadbalancer.algorithms;

import it.polimi.saefa.loadbalancer.BaseLoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancer extends BaseLoadBalancer {
    public RandomLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider) {
        super(serviceInstanceListSupplierProvider);
    }

    @Override
    protected Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            if (log.isWarnEnabled())
                log.warn("No servers available for service: " + getServiceId());
            return new EmptyResponse();
        } else {
            int index = ThreadLocalRandom.current().nextInt(serviceInstances.size());
            ServiceInstance instance = serviceInstances.get(index);
            if (log.isDebugEnabled()) {
                log.debug("RandomLoadBalancer: selected instance "+instance.getInstanceId());
            }
            return new DefaultResponse(instance);
        }
    }
}
