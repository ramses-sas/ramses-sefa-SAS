package polimi.saefa.paymentproxyservice.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentProxyService {

//	@Autowired
//	private RestaurantRepository restaurantRepository;

 	public String dummyMethod(String myString) {
		return myString;
	}
	
}

