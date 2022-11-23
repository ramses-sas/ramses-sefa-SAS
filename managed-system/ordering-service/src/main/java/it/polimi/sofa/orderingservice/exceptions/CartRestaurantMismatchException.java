package it.polimi.sofa.orderingservice.exceptions;

public class CartRestaurantMismatchException extends RuntimeException {
    public CartRestaurantMismatchException(String message) {
        super(message);
    }
}
