package polimi.saefa.apigatewayservice.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import polimi.saefa.apigatewayservice.APIGatewayApplication;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

public class LoadBalancerFilter implements GlobalFilter, Ordered {
    private final Logger log = LoggerFactory.getLogger(APIGatewayApplication.class);
    LoadBalancerClientFactory clientFactory;

    public LoadBalancerFilter(ConfigurableApplicationContext context) {
        this.clientFactory = context.getBean(LoadBalancerClientFactory.class);
    }

    @Override
    public int getOrder() {
        return LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(GATEWAY_SCHEME_PREFIX_ATTR);
        if (url == null || (!"lb".equals(url.getScheme()) && !"lb".equals(schemePrefix))) {
            return chain.filter(exchange);
        }
        // preserve the original url
        addOriginalRequestUrl(exchange, url);
        if (log.isTraceEnabled()) {
            log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName() + " url before: " + url);
        }
        URI requestUri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        assert requestUri != null;
        String serviceId = requestUri.getHost();
        Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
            .getSupportedLifecycleProcessors(clientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
                    RequestDataContext.class, ResponseData.class, ServiceInstance.class);
        DefaultRequest<RequestDataContext> lbRequest =
            new DefaultRequest<>(new RequestDataContext(new RequestData(exchange.getRequest()), getHint(serviceId)));
        LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
        return choose(lbRequest, serviceId, supportedLifecycleProcessors)
            .doOnNext(response -> {
                if (!response.hasServer()) {
                    supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                            .onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, response)));
                    throw NotFoundException.create(false, "Unable to find instance for " + url.getHost());
                }
                ServiceInstance retrievedInstance = response.getServer();
                URI uri = exchange.getRequest().getURI();
                log.info("retrievedInstance: ID {}", retrievedInstance.getInstanceId());

                // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
                // if the loadbalancer doesn't provide one.
                String overrideScheme = retrievedInstance.isSecure() ? "https" : "http";
                if (schemePrefix != null) {
                    overrideScheme = url.getScheme();
                }
                DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(retrievedInstance, overrideScheme);
                URI requestUrl = reconstructURI(serviceInstance, uri);
                exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
                exchange.getAttributes().put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
                supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, response));
            }).then(chain.filter(exchange))
            .doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                    .onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                            CompletionContext.Status.FAILED, throwable, lbRequest,
                            exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR)))))
            .doOnSuccess(aVoid -> supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
                    .onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                            CompletionContext.Status.SUCCESS, lbRequest,
                            exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR), buildResponseData(exchange,
                            loadBalancerProperties.isUseRawStatusCodeInResponseData())))));
    }




    private ResponseData buildResponseData(ServerWebExchange exchange, boolean useRawStatusCodes) {
        if (useRawStatusCodes) {
            return new ResponseData(new RequestData(exchange.getRequest()), exchange.getResponse());
        }
        return new ResponseData(exchange.getResponse(), new RequestData(exchange.getRequest()));
    }

    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }

    private Mono<Response<ServiceInstance>> choose(Request<RequestDataContext> lbRequest, String serviceId,
                                                   Set<LoadBalancerLifecycle> supportedLifecycleProcessors) {
        log.info("LoadBalancing: retrieving instance for " + serviceId);
        // TODO - punto in cui intervenire per il load balancing
        ReactorLoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance(serviceId,
                ReactorServiceInstanceLoadBalancer.class);
        if (loadBalancer == null) {
            throw new NotFoundException("No loadbalancer available for " + serviceId);
        }
        supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
        return loadBalancer.choose(lbRequest);
    }

    private String getHint(String serviceId) {
        LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
        Map<String, String> hints = loadBalancerProperties.getHint();
        String defaultHint = hints.getOrDefault("default", "default");
        String hintPropertyValue = hints.get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }
}
