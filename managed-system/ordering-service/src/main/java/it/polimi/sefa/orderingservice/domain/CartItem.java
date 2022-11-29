package it.polimi.sefa.orderingservice.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@Data
@Embeddable
@NoArgsConstructor
public class CartItem {
    private String id;
    private String name;
    private int quantity;
    private double price;
    

    public CartItem(String id, int quantity) {
        this.id = id;
        this.quantity = quantity;
    }

    public int addQuantity(int quantity){
        if (quantity<1)
            return 0;
        this.quantity += quantity;
        return this.quantity;
    }

    public int removeQuantity(int quantity){
        if (quantity<1)
            return 0;
        this.quantity -= quantity;
        return this.quantity;
    }


}