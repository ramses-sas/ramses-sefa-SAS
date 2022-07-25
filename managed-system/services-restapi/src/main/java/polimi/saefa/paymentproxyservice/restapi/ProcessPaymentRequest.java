package polimi.saefa.paymentproxyservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {
    private String cardNumber;
    private int expMonth;
    private int expYear;
    private String cvv;
    private double amount;
}

