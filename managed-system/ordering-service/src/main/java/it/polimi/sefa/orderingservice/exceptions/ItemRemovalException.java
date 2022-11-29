package it.polimi.sefa.orderingservice.exceptions;

public class ItemRemovalException extends RuntimeException {
    public ItemRemovalException(String message) {
        super(message);
    }
}
