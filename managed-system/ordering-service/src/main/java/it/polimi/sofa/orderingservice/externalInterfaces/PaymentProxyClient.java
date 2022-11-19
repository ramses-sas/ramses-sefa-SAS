package it.polimi.sofa.orderingservice.externalInterfaces;

import it.polimi.sofa.orderingservice.config.LoadBalancerConfig;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import it.polimi.sofa.paymentproxyservice.restapi.ProcessPaymentRequest;
import it.polimi.sofa.paymentproxyservice.restapi.ProcessPaymentResponse;

@FeignClient(name = "PAYMENT-PROXY-SERVICE")
@LoadBalancerClient(name = "PAYMENT-PROXY-SERVICE", configuration = LoadBalancerConfig.class)
public interface PaymentProxyClient {
    @PostMapping("/rest/processPayment")
    ProcessPaymentResponse processPayment(@RequestBody ProcessPaymentRequest request);
}
