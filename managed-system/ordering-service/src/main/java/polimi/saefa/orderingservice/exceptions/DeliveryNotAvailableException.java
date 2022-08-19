package polimi.saefa.orderingservice.exceptions;

public class DeliveryNotAvailableException extends RuntimeException{
    private Long cartId;
    public DeliveryNotAvailableException(String message, Long cartId) {
        super(message);
        this.cartId = cartId;
    }

    public Long getCartId() {
        return cartId;
    }
}
