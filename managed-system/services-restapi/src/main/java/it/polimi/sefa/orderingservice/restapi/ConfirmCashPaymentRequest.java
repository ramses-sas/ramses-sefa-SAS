package it.polimi.sefa.orderingservice.restapi;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmCashPaymentRequest {
    private String address;
    private String city;
    private int number;
    private String zipcode;
    private String telephoneNumber;
    private Date scheduledTime;
}
