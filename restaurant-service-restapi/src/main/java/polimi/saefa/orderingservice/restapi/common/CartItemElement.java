package polimi.saefa.orderingservice.restapi.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemElement {
    private String id;
    private int quantity;
}
