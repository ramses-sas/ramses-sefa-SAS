package it.polimi.saefa.loadbalancer.algorithms;

import it.polimi.saefa.loadbalancer.BaseLoadBalancer;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WeightedRandomLoadBalancer extends BaseLoadBalancer {

    // The weights of the services. Key is the instanceAddress, value is the weight.
    protected Map<String, Double> weightPerAddress;

    public WeightedRandomLoadBalancer(ServiceInstanceListSupplier serviceInstanceListSupplierProvider) {
        super(serviceInstanceListSupplierProvider);
        weightPerAddress = new HashMap<>();
    }

    public void setWeightForInstanceAtAddress(String instanceAddress, Double weight) {
        if (weight == null || weight <= 0)
            weightPerAddress.remove(instanceAddress);
        else
            weightPerAddress.put(instanceAddress, weight);
    }

    @Override
    protected Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> serviceInstances) {
        if (serviceInstances.isEmpty()) {
            if (log.isWarnEnabled())
                log.warn("No servers available for service: " + getServiceId());
            return new EmptyResponse();
        }
        if (!weightPerAddress.isEmpty() && weightPerAddress.size() < serviceInstances.size())
            throw new IllegalStateException("You don't have a weight for all the instances. You have to set a weight for all the instances or none of them.");
        double p = Math.random();
        double cumulativeProbability = 0.0;
        serviceInstances.sort((o1, o2) -> {
            Double weight1 = weightPerAddress.get(o1.getHost()+":"+o1.getPort());
            Double weight2 = weightPerAddress.get(o2.getHost()+":"+o2.getPort());
            return weight1.compareTo(weight2);
        });
        int n = serviceInstances.size();
        for (ServiceInstance instance : serviceInstances) {
            Double instanceWeight = weightPerAddress.getOrDefault(instance.getHost()+":"+instance.getPort(), 1.0/n);
            cumulativeProbability += instanceWeight;
            if (p <= cumulativeProbability) {
                if (log.isDebugEnabled())
                    log.debug("WeightedRandomLoadBalancer: selected instance "+instance.getInstanceId()+" with p="+instanceWeight);
                return new DefaultResponse(instance);
            }
        }
        // Se siamo qui, è perché la dim della lista è minore di quella della map dei pesi. Quindi la somma dei pesi è strettamente minore di 1.
        // In questo caso, l'ultimo elemento della lista è quello scelto in extremis
        return new DefaultResponse(serviceInstances.get(serviceInstances.size()-1));
    }

}
