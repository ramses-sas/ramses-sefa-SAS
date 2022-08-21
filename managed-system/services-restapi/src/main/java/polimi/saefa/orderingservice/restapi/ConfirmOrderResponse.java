package polimi.saefa.orderingservice.restapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderResponse {
    boolean confirmed = false;
    Boolean requiresCashPayment = false;
    Boolean isTakeAway = false;

    public ConfirmOrderResponse(boolean confirmed) {
        this.confirmed = confirmed;
    }
}
