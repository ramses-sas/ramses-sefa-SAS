package it.polimi.saefa.deliveryproxy1service.externalinterface;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliverRequest {
    private String address;
    private String city;
    private int number;
    private String zipcode;
    private String telephoneNumber;
    private Date scheduledTime;
}

