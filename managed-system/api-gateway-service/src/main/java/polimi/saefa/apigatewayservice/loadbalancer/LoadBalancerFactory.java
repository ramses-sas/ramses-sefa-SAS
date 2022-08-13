package polimi.saefa.apigatewayservice.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.event.EventListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.WeightedRoundRobinLoadBalancer;

import java.util.*;

@Slf4j
@Service
public class LoadBalancerFactory implements ReactiveLoadBalancer.Factory<ServiceInstance> {

    @Autowired
    DiscoveryClient discoveryClient;

    @Autowired
    Environment environment;

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

    public void create(LoadBalancerType loadBalancerType, String serviceId) {
        EurekaInstanceListSupplier supplier = new EurekaInstanceListSupplier(discoveryClient, environment, serviceId);
        try {
            this.register(loadBalancerType.getLoadBalancerClass().getDeclaredConstructor(ServiceInstanceListSupplier.class).newInstance(supplier));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void create(LoadBalancerType loadBalancerType, ServiceInstanceListSupplier supplier) {
        try {
            this.register(loadBalancerType.getLoadBalancerClass().getDeclaredConstructor(ServiceInstanceListSupplier.class).newInstance(supplier));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void createIfNeeded(LoadBalancerType loadBalancerType, String serviceId) {
        if (!loadBalancers.containsKey(serviceId) || loadBalancers.get(serviceId) == null ||
                loadBalancers.get(serviceId).getClass() == loadBalancerType.getLoadBalancerClass()) {
            this.create(loadBalancerType, serviceId);
        }
    }

    public void createIfNeeded(LoadBalancerType loadBalancerType, ServiceInstanceListSupplier supplier) {
        if (!loadBalancers.containsKey(supplier.getServiceId()) || loadBalancers.get(supplier.getServiceId()) == null ||
                loadBalancers.get(supplier.getServiceId()).getClass() == loadBalancerType.getLoadBalancerClass()) {
            this.create(loadBalancerType, supplier);
        }
    }


    @EventListener(value = EnvironmentChangeEvent.class)
    public void register(EnvironmentChangeEvent environmentChangeEvent) {
        // EXAMPLE: Received an environment changed event for keys [config.client.version, test.property]
        log.info("Received an environment changed event for keys {}", environmentChangeEvent.getKeys());
        List<String> lbKeys = environmentChangeEvent.getKeys().stream()
                .map(key -> key.startsWith("loadbalancing.") ? key.replace("loadbalancing.","") : null)
                .filter(Objects::nonNull).distinct().toList();
        lbKeys.forEach(key -> {
            String value = Objects.requireNonNull(environment.getProperty(key));
            String[] keyParts = key.split("\\.");
            String serviceName = keyParts[0];
            String identifier = keyParts[1]; // localhost_PORT oppure UNDERSCORE-SEPARATED-IP_PORT oppure global
            String[] propertyElements = Arrays.copyOfRange(keyParts, 2, keyParts.length);
            if (!identifier.equals("global")) {
                int lastIndex = identifier.lastIndexOf("_");
                if (lastIndex == -1) { throw new RuntimeException("Invalid identifier: " + identifier); }
                String host = identifier.substring(0, lastIndex).replace("_", ".");
                String port = identifier.substring(lastIndex + 1);
                if (propertyElements.length == 1 && propertyElements[0].equals("type")) {
                    log.info("Changing load balancer type for service {} to {}", serviceName, value);
                    switch (value) {
                        case "ROUND_ROBIN" -> this.createIfNeeded(LoadBalancerType.ROUND_ROBIN, serviceName);
                        case "WEIGHTED_ROUND_ROBIN" -> this.createIfNeeded(LoadBalancerType.WEIGHTED_ROUND_ROBIN, serviceName);
                        default -> throw new RuntimeException("Unknown load balancer type: " + value);
                    }
                } else if (propertyElements.length == 1 && propertyElements[0].equals("weight")) {
                    log.info("Changing load balancer weight for service {} to {}", serviceName, value);
                    int weight = Integer.parseInt(value);
                    BaseLoadBalancer lb = loadBalancers.get(serviceName);
                    if (lb instanceof WeightedRoundRobinLoadBalancer) {
                        ((WeightedRoundRobinLoadBalancer) lb).setWeight(host+":"+port, weight);
                    }
                }
            } else {
                log.info("Global property found: {}", key);
                if (propertyElements.length == 1 && propertyElements[0].equals("type")) {
                    switch (value) {
                        case "ROUND_ROBIN" -> this.createIfNeeded(LoadBalancerType.ROUND_ROBIN, serviceName);
                        case "WEIGHTED_ROUND_ROBIN" -> this.createIfNeeded(LoadBalancerType.WEIGHTED_ROUND_ROBIN, serviceName);
                        default -> throw new RuntimeException("Unknown load balancer type: " + value);
                    }
                }
                log.warn("Must still be implemented");
            }
        });
    }









    // Implementation of ReactorLoadBalancer interface

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
