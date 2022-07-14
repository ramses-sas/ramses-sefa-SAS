package polimi.saefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class DeliveryInfo
{
    private String address;
    private String city;
    private int number;
    private String zipcode;
    private String telephoneNumber;
    private Date scheduledTime;
}
