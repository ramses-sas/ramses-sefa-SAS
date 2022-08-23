package it.polimi.saefa.deliveryproxy2service.rest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import it.polimi.saefa.deliveryproxyservice.restapi.DeliverOrderRequest;
import it.polimi.saefa.deliveryproxyservice.restapi.DeliverOrderResponse;
import it.polimi.saefa.deliveryproxy2service.domain.*;

import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/")
public class DeliveryProxyRestController {

	@Autowired
	private DeliveryProxyService deliveryProxyService;
	
    private final Logger logger = Logger.getLogger(DeliveryProxyRestController.class.toString());

	@PostMapping(path = "deliverOrder")
	public DeliverOrderResponse deliverOrder(@RequestBody DeliverOrderRequest request) {
		logger.info("REST CALL: deliverOrder from proxy 2 to " + request.getAddress());
		return new DeliverOrderResponse(deliveryProxyService.deliverOrder(request.getAddress(), request.getCity(), request.getNumber(), request.getZipcode(), request.getTelephoneNumber(), request.getScheduledTime()));
	}

}