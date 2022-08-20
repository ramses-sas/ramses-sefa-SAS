package it.polimi.saefa.loadbalancer.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.core.ResolvableType;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class LoadBalancerFactory implements ReactorLoadBalancer.Factory<ServiceInstance> {

    private final LoadBalancerClientsProperties properties;
    private final Map<String, BaseLoadBalancer> loadBalancers;

    public LoadBalancerFactory() {
        this.properties = new LoadBalancerClientsProperties();
        loadBalancers = new HashMap<>();
    }

    public LoadBalancerFactory(LoadBalancerClientsProperties properties) {
        this.properties = properties;
        loadBalancers = new HashMap<>();
    }

    public Map<String, BaseLoadBalancer> getLoadBalancers() {
        return loadBalancers;
    }

    // Use to register a load balancer
    public void register(BaseLoadBalancer loadBalancer) {
        loadBalancers.put(loadBalancer.getServiceId(), loadBalancer);
    }

    public BaseLoadBalancer create(LoadBalancerType loadBalancerType, ServiceInstanceListSupplier supplier) {
        log.info("LoadBalancerFactory: creating load balancer of type " + loadBalancerType + " for service " + supplier.getServiceId());
        try {
            BaseLoadBalancer lb = loadBalancerType.getLoadBalancerClass().getDeclaredConstructor(ServiceInstanceListSupplier.class).newInstance(supplier);
            this.register(lb);
            return lb;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public BaseLoadBalancer createIfNeeded(LoadBalancerType loadBalancerType, ServiceInstanceListSupplier supplier) {
        if (!loadBalancers.containsKey(supplier.getServiceId()) || loadBalancers.get(supplier.getServiceId()) == null ||
                loadBalancers.get(supplier.getServiceId()).getClass() != loadBalancerType.getLoadBalancerClass()) {
            return this.create(loadBalancerType, supplier);
        }
        return loadBalancers.get(supplier.getServiceId());
    }






    /*
    public BaseLoadBalancer create(LoadBalancerType loadBalancerType, String serviceId) {
        EurekaInstanceListSupplier supplier = new EurekaInstanceListSupplier(discoveryClient, environment, serviceId);
        return this.create(loadBalancerType, supplier);
    }
    public BaseLoadBalancer createIfNeeded(LoadBalancerType loadBalancerType, String serviceId) {
        if (!loadBalancers.containsKey(serviceId) || loadBalancers.get(serviceId) == null ||
                loadBalancers.get(serviceId).getClass() != loadBalancerType.getLoadBalancerClass()) {
            return this.create(loadBalancerType, serviceId);
        }
        return loadBalancers.get(serviceId);
    }*/




    // Implementation of ReactorLoadBalancer interface

    @Override
    public ReactorLoadBalancer<ServiceInstance> getInstance(String serviceId) {
        return this.getInstance(serviceId, BaseLoadBalancer.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(String name, Class<?> clazz, Class<?>... generics) {
        ResolvableType type = ResolvableType.forClassWithGenerics(clazz, generics);
        BaseLoadBalancer loadBalancer = loadBalancers.get(name);
        if (Objects.requireNonNull(type.resolve()).isAssignableFrom(loadBalancer.getClass())) {
            return (T) loadBalancer;
        }
        return null;
    }

    @Override
    public <X> Map<String, X> getInstances(String name, Class<X> type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LoadBalancerProperties getProperties(String serviceId) {
        if (serviceId == null || !properties.getClients().containsKey(serviceId)) {
            return properties;
        }
        return properties.getClients().get(serviceId);
    }
}


