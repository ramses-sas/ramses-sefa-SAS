package it.polimi.sefa.orderingservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveItemFromCartRequest {
    private long cartId=0;
    private Long restaurantId;
    private String itemId;
    private int quantity;
}
