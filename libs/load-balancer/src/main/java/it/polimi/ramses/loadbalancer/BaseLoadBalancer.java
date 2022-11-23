package it.polimi.ramses.loadbalancer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.List;

public abstract class BaseLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    protected final Log log = LogFactory.getLog(this.getClass().getName());
    private final ServiceInstanceListSupplier serviceInstanceListSupplierProvider;

    public BaseLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return serviceInstanceListSupplierProvider.get(request).next().map(this::processInstanceResponse);
    }

    protected abstract Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> serviceInstances);

    public String getServiceId() {
        return serviceInstanceListSupplierProvider.getServiceId();
    }
}
