package it.polimi.saefa.orderingservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderRequest {
    private Long cartId;
    private String cardNumber;
    private int expMonth;
    private int expYear;
    private String cvv;
    private String address;
    private String city;
    private int number;
    private String zipcode;
    private String telephoneNumber;
    private Date scheduledTime;
}

