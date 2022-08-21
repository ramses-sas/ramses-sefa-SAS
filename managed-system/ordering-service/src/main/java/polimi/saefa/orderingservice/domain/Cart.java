package polimi.saefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import polimi.saefa.orderingservice.exceptions.CartRestaurantMismatchException;

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
    private Long restaurantId;
    private double totalPrice = 0;
    @ElementCollection
    private Map<String, CartItem> items;

    private boolean paid = false;

    private boolean requiresCashPayment = false;
    private boolean requiresTakeaway = false;

    public Cart(Long restaurantId) {
        this.restaurantId = restaurantId;
        items = new HashMap<>();
    }

    public Cart(Long restaurantId, Map<String, CartItem> items) {
        this.restaurantId = restaurantId;
        this.items = items;
    }

    public boolean addItem(String itemId, Long restaurantId, int quantity) {
        if (!paid)
            if(Objects.equals(restaurantId, this.restaurantId)) {
                CartItem item = items.get(itemId);
                if (item != null)
                    item.addQuantity(quantity);
                else
                    items.put(itemId, new CartItem(itemId, quantity));
                return true;
            }
        else throw new CartRestaurantMismatchException("The item specified belongs to a different restaurant");
        return false;
    }

    public boolean removeItem(String itemId, Long restaurantId, int quantity){
        if(!paid)
            if(Objects.equals(restaurantId, this.restaurantId)) {
                CartItem item = items.get(itemId);
                if (item != null)
                    if (item.getQuantity() == quantity)
                        items.remove(itemId);
                    else
                        item.removeQuantity(quantity);
                else
                    return false;
                return true;
            } else throw new CartRestaurantMismatchException("The item specified belongs to a different restaurant");
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
