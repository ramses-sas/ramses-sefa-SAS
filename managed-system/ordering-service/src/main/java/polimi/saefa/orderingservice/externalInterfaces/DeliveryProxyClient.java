package polimi.saefa.orderingservice.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import polimi.saefa.deliveryproxyservice.restapi.DeliverOrderRequest;
import polimi.saefa.deliveryproxyservice.restapi.DeliverOrderResponse;

@FeignClient(name = "delivery-proxy-service")

public interface DeliveryProxyClient {
    @PostMapping("/rest/deliverOrder")
    DeliverOrderResponse deliverOrder(@RequestBody DeliverOrderRequest request);
}
