package it.polimi.saefa.orderingservice.externalInterfaces;

import it.polimi.saefa.orderingservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import it.polimi.saefa.deliveryproxyservice.restapi.DeliverOrderRequest;
import it.polimi.saefa.deliveryproxyservice.restapi.DeliverOrderResponse;

@FeignClient(name = "DELIVERY-PROXY-SERVICE")
@LoadBalancerClient(name = "DELIVERY-PROXY-SERVICE", configuration = LoadBalancerConfig.class)
public interface DeliveryProxyClient {
    @PostMapping("/rest/deliverOrder")
    DeliverOrderResponse deliverOrder(@RequestBody DeliverOrderRequest request);
}
