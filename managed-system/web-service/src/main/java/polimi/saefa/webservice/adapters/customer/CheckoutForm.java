package polimi.saefa.webservice.adapters.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutForm {
	private String cardNumber;
	private String expMonth;
	private String expYear;
	private String cvv;
	private String address;
	private String city;
	private String number;
	private String zipcode;
	private String telephoneNumber;
	private String scheduledTime;
}
