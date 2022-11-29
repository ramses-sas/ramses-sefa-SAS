package it.polimi.sefa.orderingservice.exceptions;

public class CartRestaurantMismatchException extends RuntimeException {
    public CartRestaurantMismatchException(String message) {
        super(message);
    }
}
