package polimi.saefa.apigatewayservice.loadbalancer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


public class EurekaInstanceListSupplier implements ServiceInstanceListSupplier {
    public static final String SERVICE_DISCOVERY_TIMEOUT = "spring.cloud.loadbalancer.service-discovery.timeout";
    private static final Log LOG = LogFactory.getLog(EurekaInstanceListSupplier.class);
    private Duration timeout = Duration.ofSeconds(10);
    private final String serviceId;
    private final Flux<List<ServiceInstance>> serviceInstances;

    public EurekaInstanceListSupplier(DiscoveryClient delegate, Environment environment, String serviceId) {
        this.serviceId = serviceId;
        resolveTimeout(environment);
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

    private void resolveTimeout(Environment environment) {
        String providedTimeout = environment.getProperty(SERVICE_DISCOVERY_TIMEOUT);
        if (providedTimeout != null) {
            timeout = DurationStyle.detectAndParse(providedTimeout);
        }
    }

    private void logTimeout() {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Timeout occurred while retrieving instances for service %s."
                    + "The instances could not be retrieved during %s", serviceId, timeout));
        }
    }

    private void logException(Throwable error) {
        if (LOG.isErrorEnabled()) {
            LOG.error(String.format("Exception occurred while retrieving instances for service %s", serviceId), error);
        }
    }

}
