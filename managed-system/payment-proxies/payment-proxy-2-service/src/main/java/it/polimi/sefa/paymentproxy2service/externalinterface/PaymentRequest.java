package it.polimi.sefa.paymentproxy2service.externalinterface;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String cardNumber;
    private int expMonth;
    private int expYear;
    private String cvv;
    private double amount;
}

