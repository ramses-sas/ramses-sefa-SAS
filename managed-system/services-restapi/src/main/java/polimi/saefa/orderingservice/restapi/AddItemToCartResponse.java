package polimi.saefa.orderingservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddItemToCartResponse {
    private Long id;
    private Long restaurantId;
    private double totalPrice = 0;
    private Collection<CartItemElement> items;

}
