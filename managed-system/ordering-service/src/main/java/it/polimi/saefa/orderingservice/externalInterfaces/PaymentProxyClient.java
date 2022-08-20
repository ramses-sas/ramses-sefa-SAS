package it.polimi.saefa.orderingservice.externalInterfaces;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import it.polimi.saefa.paymentproxyservice.restapi.ProcessPaymentRequest;
import it.polimi.saefa.paymentproxyservice.restapi.ProcessPaymentResponse;

@FeignClient(name = "payment-proxy-service")
public interface PaymentProxyClient {
    @PostMapping("/rest/processPayment")
    ProcessPaymentResponse processPayment(@RequestBody ProcessPaymentRequest request);
}
