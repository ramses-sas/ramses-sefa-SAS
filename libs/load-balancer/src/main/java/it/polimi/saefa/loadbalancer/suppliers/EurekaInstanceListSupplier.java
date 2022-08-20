package it.polimi.saefa.loadbalancer.suppliers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EurekaInstanceListSupplier implements ServiceInstanceListSupplier {
    private final Duration timeout;
    private final String serviceId;
    private final Flux<List<ServiceInstance>> serviceInstances;

    public EurekaInstanceListSupplier(DiscoveryClient delegate, String serviceId) {
        this(delegate, serviceId, Duration.ofSeconds(10));
    }

    public EurekaInstanceListSupplier(DiscoveryClient delegate, String serviceId, Duration timeout) {
        this.serviceId = serviceId;
        this.timeout = timeout;
        this.serviceInstances = Flux.defer(() -> Mono.fromCallable(() -> delegate.getInstances(serviceId)))
            .timeout(timeout, Flux.defer(() -> {
                logTimeout();
                return Flux.just(new ArrayList<>());
            }), Schedulers.boundedElastic()).onErrorResume(error -> {
                logException(error);
                return Flux.just(new ArrayList<>());
            });
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public Flux<List<ServiceInstance>> get(Request request) {
        return serviceInstances;
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return serviceInstances;
    }


    private void logTimeout() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Timeout occurred while retrieving instances for service %s."
                    + "The instances could not be retrieved during %s", serviceId, timeout));
        }
    }

    private void logException(Throwable error) {
        if (log.isErrorEnabled()) {
            log.error(String.format("Exception occurred while retrieving instances for service %s", serviceId), error);
        }
    }

}
