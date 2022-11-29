package it.polimi.sefa.paymentproxy1service.rest;

import it.polimi.sefa.paymentproxy1service.domain.PaymentProxyService;
import it.polimi.sefa.paymentproxyservice.restapi.ProcessPaymentRequest;
import it.polimi.sefa.paymentproxyservice.restapi.ProcessPaymentResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/")
public class PaymentProxyRestController {

	@Autowired
	private PaymentProxyService paymentProxyService;
	
    private final Logger logger = Logger.getLogger(PaymentProxyRestController.class.toString());

	@PostMapping(path = "processPayment")
	public ProcessPaymentResponse processPayment(@RequestBody ProcessPaymentRequest request) {
		logger.info("REST CALL: processPayment from proxy 1 with card " + request.getCardNumber());
		return new ProcessPaymentResponse(paymentProxyService.processPayment(request.getCardNumber(), request.getExpMonth(), request.getExpYear(), request.getCvv(), request.getAmount()));
	}


}
