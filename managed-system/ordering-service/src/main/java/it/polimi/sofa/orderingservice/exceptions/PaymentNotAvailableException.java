package it.polimi.sofa.orderingservice.exceptions;

public class PaymentNotAvailableException extends RuntimeException{
    private Long cartId;
    public PaymentNotAvailableException(String message, Long cartId) {
        super(message);
        this.cartId = cartId;
    }

    public Long getCartId() {
        return cartId;
    }
}
