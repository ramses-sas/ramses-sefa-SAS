package polimi.saefa.apigatewayservice.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class LoadBalancerFactory implements ReactiveLoadBalancer.Factory<ServiceInstance> {
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

    // Use to register a load balancer
    public void register(BaseLoadBalancer loadBalancer) {
        loadBalancers.put(loadBalancer.getServiceId(), loadBalancer);
    }

    @Override
    public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
        return getInstance(serviceId, BaseLoadBalancer.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(String name, Class<?> clazz, Class<?>... generics) {
        ResolvableType type = ResolvableType.forClassWithGenerics(clazz, generics);
        BaseLoadBalancer loadBalancer = loadBalancers.get(name);
        if (loadBalancer.getClass().isAssignableFrom(Objects.requireNonNull(type.resolve()))) {
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
            // no specific client properties, return default
            return properties;
        }
        // because specifics are overlayed on top of defaults, everything in `properties`,
        // unless overridden, is in `clientsProperties`
        return properties.getClients().get(serviceId);
    }
}
