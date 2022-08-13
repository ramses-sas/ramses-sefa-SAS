package polimi.saefa.apigatewayservice.archivecode;
/*
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.*;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;

public class FullLoadBalancerFilter implements GlobalFilter, Ordered {
    private final Logger log = LoggerFactory.getLogger(FullLoadBalancerFilter.class);
    LoadBalancerClientFactory clientFactory;

    public FullLoadBalancerFilter(ConfigurableApplicationContext context) {
        this.clientFactory = context.getBean(LoadBalancerClientFactory.class);
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
        Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
            .getSupportedLifecycleProcessors(clientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
                    RequestDataContext.class, ResponseData.class, ServiceInstance.class);
        log.info(supportedLifecycleProcessors.size() + " lifecycle processors found for service " + serviceId);
        DefaultRequest<RequestDataContext> lbRequest =
            new DefaultRequest<>(new RequestDataContext(new RequestData(exchange.getRequest()), getHint(serviceId)));
        // LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
        return choose(lbRequest, serviceId, supportedLifecycleProcessors)
            .doOnNext(response -> {
                if (!response.hasServer()) {
                    supportedLifecycleProcessors.forEach(lifecycle ->
                            lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, response)));
                    throw NotFoundException.create(false, "Unable to find instance for " + requestUrl.getHost());
                }
                ServiceInstance retrievedInstance = response.getServer();
                URI finalUrl = reconstructURI(retrievedInstance, requestUrl);
                log.info("LoadBalancing: selected instance {}", retrievedInstance.getInstanceId());
                log.info("LoadBalancing: the final URL is {}", finalUrl);
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, finalUrl);
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
                supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, response));
            })
            .then(chain.filter(exchange))
            .doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                    .onComplete(completionHandler(exchange, lbRequest, throwable))))
            .doOnSuccess(aVoid -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                    .onComplete(completionHandler(exchange, lbRequest, null))));
    }




    private Mono<Response<ServiceInstance>> choose(Request<RequestDataContext> lbRequest, String serviceId,
                                                   Set<LoadBalancerLifecycle> supportedLifecycleProcessors) {
        log.info("LoadBalancing: retrieving instance for " + serviceId);
        // TODO - punto in cui intervenire per il load balancing
        ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(serviceId, ReactorServiceInstanceLoadBalancer.class);
        if (loadBalancer == null) {
            throw new NotFoundException("No loadbalancer available for " + serviceId);
        }
        log.info("LoadBalancing: using load balancer " + System.identityHashCode(loadBalancer));
        supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
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

    private CompletionContext completionHandler(ServerWebExchange exchange, DefaultRequest<RequestDataContext> request, Throwable error) {
        if (error != null) {
            return new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                    CompletionContext.Status.FAILED, error, request,
                    exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR));
        }
        ResponseData res = new ResponseData(exchange.getResponse(), new RequestData(exchange.getRequest()));
        return new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                CompletionContext.Status.SUCCESS, request,
                exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR), res);
    }
}
*/
