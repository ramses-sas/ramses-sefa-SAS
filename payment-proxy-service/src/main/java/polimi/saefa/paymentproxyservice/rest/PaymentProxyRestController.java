package polimi.saefa.paymentproxyservice.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import polimi.saefa.paymentproxyservice.domain.*;

import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/")
public class PaymentProxyRestController {

	@Autowired
	private PaymentProxyService paymentProxyService;
	
    private final Logger logger = Logger.getLogger(PaymentProxyRestController.class.toString());



}
