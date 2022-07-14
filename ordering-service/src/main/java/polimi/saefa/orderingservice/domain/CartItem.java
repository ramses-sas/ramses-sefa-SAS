package polimi.saefa.orderingservice.domain;

import lombok.Data;

@Data
public class CartItem{
    private String id;
    private int quantity;

    public CartItem(String id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }
}