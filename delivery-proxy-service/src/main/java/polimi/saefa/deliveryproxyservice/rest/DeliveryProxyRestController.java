package polimi.saefa.deliveryproxyservice.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import polimi.saefa.deliveryproxyservice.domain.*;

import java.util.logging.Logger;

@RestController
@RequestMapping(path="/rest/")
public class DeliveryProxyRestController {

	@Autowired
	private DeliveryProxyService deliveryProxyService;
	
    private final Logger logger = Logger.getLogger(DeliveryProxyRestController.class.toString());



}
