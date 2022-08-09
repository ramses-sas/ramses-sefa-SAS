package polimi.saefa.apigatewayservice.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import polimi.saefa.apigatewayservice.loadbalancer.algorithms.RoundRobinLoadBalancer;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.Map;
import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;


public class LoadBalancerFilter implements GlobalFilter, Ordered {
    private final Logger log = LoggerFactory.getLogger(LoadBalancerFilter.class);
    ReactiveLoadBalancer.Factory<ServiceInstance> clientFactory;

    public LoadBalancerFilter(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
        clientFactory = factory;
        //this.clientFactory = context.getBean(LoadBalancerClientFactory.class);
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI requestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        if (requestUrl == null || (!"lb".equals(requestUrl.getScheme()) && !"lb".equals(schemePrefix)))
            return chain.filter(exchange);
        // preserve the original url
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, requestUrl);

        String serviceId = requestUrl.getHost();
        DefaultRequest<RequestDataContext> lbRequest =
                new DefaultRequest<>(new RequestDataContext(new RequestData(exchange.getRequest()), getHint(serviceId)));
        // LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
        return choose(lbRequest, serviceId)
            .doOnNext(response -> {
                if (!response.hasServer())
                    throw NotFoundException.create(false, "Unable to find instance for " + requestUrl.getHost());
                ServiceInstance retrievedInstance = response.getServer();
                URI finalUrl = reconstructURI(retrievedInstance, requestUrl);
                log.info("LoadBalancing: selected instance {}", retrievedInstance.getInstanceId());
                log.info("LoadBalancing: the final URL is {}", finalUrl);
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, finalUrl);
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
            })
            .then(chain.filter(exchange));
    }




    private Mono<Response<ServiceInstance>> choose(Request<RequestDataContext> lbRequest, String serviceId) {
        log.info("LoadBalancing: retrieving instance for " + serviceId);
        // TODO - punto in cui intervenire per il load balancing
        ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(serviceId, RoundRobinLoadBalancer.class);
        if (loadBalancer == null) {
            throw new NotFoundException("No loadbalancer available for " + serviceId);
        }
        log.info("LoadBalancing: using load balancer " + loadBalancer.getClass().getName() + " " + System.identityHashCode(loadBalancer));
        return loadBalancer.choose(lbRequest);
    }

    protected URI reconstructURI(ServiceInstance selectedInstance, URI original) {
        String targetScheme = selectedInstance.isSecure() ? "https" : "http";
        ServiceInstance targetInstance = new DelegatingServiceInstance(selectedInstance, targetScheme); // To use the scheme of the target instance
        return LoadBalancerUriTools.reconstructURI(targetInstance, original);
    }

    private String getHint(String serviceId) {
        LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
        Map<String, String> hints = loadBalancerProperties.getHint();
        String defaultHint = hints.getOrDefault("default", "default");
        String hintPropertyValue = hints.get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }

}
