package polimi.saefa.loadbalancer.suppliers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InstanceListSupplierFactory {

    // Map <serviceId, ServiceInstanceListSupplier>
    private final Map<String, ServiceInstanceListSupplier> suppliers;
    private final DiscoveryClient discoveryClient; // Optional. Null if no discoveryClient is provided.

    public InstanceListSupplierFactory() {
        this.suppliers = new HashMap<>();
        discoveryClient = null;
    }

    public InstanceListSupplierFactory(DiscoveryClient discoveryClient) {
        this.suppliers = new HashMap<>();
        this.discoveryClient = discoveryClient;
    }

    public ServiceInstanceListSupplier getSupplier(String serviceId) {
        return suppliers.get(serviceId);
    }

    public void register(ServiceInstanceListSupplier supplier) {
        log.info("InstanceListSupplierFactory: registering "+supplier.getClass().getSimpleName()+" for "+supplier.getServiceId());
        suppliers.put(supplier.getServiceId(), supplier);
    }

    public ServiceInstanceListSupplier createEurekaSupplier(String serviceId) {
        if (discoveryClient == null)
            throw new IllegalStateException("DiscoveryClient is not available");
        ServiceInstanceListSupplier supplier = new EurekaInstanceListSupplier(discoveryClient, serviceId);
        this.register(supplier);
        return supplier;
    }

    public ServiceInstanceListSupplier createEurekaSupplierIfNeeded(String serviceId) {
        if (suppliers.containsKey(serviceId))
            return suppliers.get(serviceId);
        return this.createEurekaSupplier(serviceId);
    }

}
