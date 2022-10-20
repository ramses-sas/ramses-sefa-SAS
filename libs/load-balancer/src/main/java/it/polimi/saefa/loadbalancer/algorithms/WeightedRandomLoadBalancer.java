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
        double p = Math.random();
        double cumulativeProbability = 0.0;
        // Sort in increasing order of weight
        serviceInstances.sort((o1, o2) -> {
            Double weight1 = weightPerAddress.get(o1.getHost()+":"+o1.getPort());
            Double weight2 = weightPerAddress.get(o2.getHost()+":"+o2.getPort());
            if (weight1 == null && weight2 == null)
                return 0;
            if (weight1 == null)
                return -1;
            if (weight2 == null)
                return 1;
            return weight1.compareTo(weight2);
        });
        int n = serviceInstances.size();
        for (ServiceInstance instance : serviceInstances) {
            Double instanceWeight;
            // If there are no weights, all the instances have the same weight.
            if (weightPerAddress.isEmpty())
                instanceWeight = 1.0 / n;
            else {
                // If there is no weight for the instance skip it
                if (!weightPerAddress.containsKey(instance.getHost()+":"+instance.getPort())) {
                    log.warn("You don't have the weight for the instance: " + instance.getInstanceId());
                    continue;
                }
                instanceWeight = weightPerAddress.get(instance.getHost() + ":" + instance.getPort());
            }
            cumulativeProbability += instanceWeight;
            if (p <= cumulativeProbability) {
                if (log.isDebugEnabled())
                    log.debug("WeightedRandomLoadBalancer: selected instance "+instance.getInstanceId()+" with p="+instanceWeight);
                return new DefaultResponse(instance);
            }
        }
        // Se siamo qui è perché la cumulativeProbability finale è strettamente minore di 1
        // (caso in cui un'istanza fallisce o semplicemente non è nella lista per qualche motivo, ma c'è il suo peso).
        // In questo caso, l'ultimo elemento della lista è quello scelto in extremis
        return new DefaultResponse(serviceInstances.get(serviceInstances.size()-1));
    }

}
