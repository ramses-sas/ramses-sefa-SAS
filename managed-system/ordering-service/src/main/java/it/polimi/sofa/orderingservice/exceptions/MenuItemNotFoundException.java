package it.polimi.sofa.orderingservice.exceptions;

public class MenuItemNotFoundException extends RuntimeException{
    public MenuItemNotFoundException(String message) {
        super(message);
    }
}
