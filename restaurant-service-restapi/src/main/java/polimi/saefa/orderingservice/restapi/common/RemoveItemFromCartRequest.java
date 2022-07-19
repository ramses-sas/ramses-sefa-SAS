package polimi.saefa.orderingservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveItemFromCartRequest {
    private long cartId=0;
    private String restaurantId;
    private String itemId;
    private int quantity;
}
