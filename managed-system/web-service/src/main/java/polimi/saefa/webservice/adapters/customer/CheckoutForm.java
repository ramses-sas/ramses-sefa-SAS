package polimi.saefa.webservice.adapters.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutForm {
	private String cardNumber;
	private int expMonth;
	private int expYear;
	private String cvv;
	private String address;
	private String city;
	private int number;
	private String zipcode;
	private String telephoneNumber;
	private String scheduledTime;
}
