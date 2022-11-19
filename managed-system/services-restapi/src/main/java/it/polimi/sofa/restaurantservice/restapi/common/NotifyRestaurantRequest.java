package it.polimi.sofa.restaurantservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotifyRestaurantRequest {
    private Long orderNumber;
    private boolean isTakeaway = false;
    private boolean needsCashPayment = false;
    private double totalPrice;

    public NotifyRestaurantRequest(Long orderNumber, double totalPrice) {
        this.orderNumber = orderNumber;
        this.totalPrice = totalPrice;
    }
}
