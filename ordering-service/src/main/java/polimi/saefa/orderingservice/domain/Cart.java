package polimi.saefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.util.function.Tuple2;

import javax.persistence.Embeddable;
import javax.persistence.Tuple;
import java.util.LinkedList;
import java.util.List;

@Embeddable
@Data
@NoArgsConstructor
public class Cart {
    private static int count = 0;
    private int id;
    private String restaurantId;
    private double totalPrice = 0;
    private List<CartItem> items;

    public Cart(String restaurantId) {
        this.id = ++count;
        this.restaurantId = restaurantId;
        items = new LinkedList<>();
    }

    public Cart(int id, String restaurantId, List<CartItem> items) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.items = items;
    }

    public boolean addItem(String itemId, String restaurantId, int quantity){
        if(restaurantId.equals(this.restaurantId)){
            items.add(new CartItem(itemId, quantity));
            return true;
        }
        return false;
    }
}
