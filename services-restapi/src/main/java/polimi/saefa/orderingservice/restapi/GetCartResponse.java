package polimi.saefa.orderingservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetCartResponse {
    private Long id;
    private Long restaurantId;
    private double totalPrice = 0;
    private Collection<CartItemElementExtended> items;
}
