package polimi.saefa.deliveryproxyservice2.externalinterface;

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

