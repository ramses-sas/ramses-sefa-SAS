package polimi.saefa.orderingservice.exceptions;

public class MenuItemNotFoundException extends RuntimeException{
    public MenuItemNotFoundException(String message) {
        super(message);
    }
}
