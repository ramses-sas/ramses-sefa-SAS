package polimi.saefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import polimi.saefa.orderingservice.restapi.common.CartItemElement;
import reactor.util.function.Tuple2;

import javax.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Embeddable
@Data
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue
    private Long id;
    private String restaurantId;
    private double totalPrice = 0;
    @ElementCollection
    private Map<String, CartItem> items;

    private boolean paid = false;

    public Cart(String restaurantId) {
        this.restaurantId = restaurantId;
        items = new HashMap<>();
    }

    public Cart(String restaurantId, Map<String, CartItem> items) {
        this.restaurantId = restaurantId;
        this.items = items;
    }

    public boolean addItem(String itemId, String restaurantId, int quantity){
        if(!paid && restaurantId.equals(this.restaurantId)){
            CartItem item = items.get(itemId);
            if(item != null)
                item.addQuantity(quantity);
            else
                items.put(itemId, new CartItem(itemId, quantity));
            return true;
        }
        return false;
    }

    public List<CartItem> getItemList(){
        Set<String> IDs = items.keySet();
        return IDs
                .stream()
                .map(id -> items.get(id))
                .collect(Collectors.toList());

    }

}
