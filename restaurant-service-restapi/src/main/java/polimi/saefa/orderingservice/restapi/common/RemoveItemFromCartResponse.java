package polimi.saefa.orderingservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveItemFromCartResponse {
    private Long id;
    private String restaurantId;
    private double totalPrice = 0;
    private Collection<CartItemElement> items;
}
