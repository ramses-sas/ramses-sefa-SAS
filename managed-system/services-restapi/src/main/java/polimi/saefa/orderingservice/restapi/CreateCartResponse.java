package polimi.saefa.orderingservice.restapi;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;

@Data
public class CreateCartResponse {
    private Long id;
    private Long restaurantId;
    private double totalPrice = 0;
    private Collection<CartItemElement> items;

    public CreateCartResponse(Long id, Long restaurantId) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.items = new ArrayList<>();
    }
}
