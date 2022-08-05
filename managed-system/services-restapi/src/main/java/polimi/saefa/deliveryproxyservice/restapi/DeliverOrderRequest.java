package polimi.saefa.deliveryproxyservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliverOrderRequest {
    String address;
    String city;
    int number;
    String zipcode;
    String telephoneNumber;
    Date scheduledTime;
    Long restaurantId;
    Long orderId;
}

