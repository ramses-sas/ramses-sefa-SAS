package polimi.saefa.orderingservice.domain;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import polimi.saefa.orderingservice.exceptions.CartNotFoundException;
import polimi.saefa.orderingservice.exceptions.CartRestaurantMismatchException;
import polimi.saefa.orderingservice.exceptions.MenuItemNotFoundException;
import polimi.saefa.orderingservice.exceptions.PaymentDetailsNotValidException;

@ControllerAdvice
public class ApplicationExceptionHandler {

    
    @ExceptionHandler(CartNotFoundException.class)
    @ResponseBody
    public ResponseEntity<String> processException(CartNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(CartRestaurantMismatchException.class)
    @ResponseBody
    public ResponseEntity<String> processException(CartRestaurantMismatchException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MenuItemNotFoundException.class)
    @ResponseBody
    public ResponseEntity<String> processException(MenuItemNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentDetailsNotValidException.class)
    @ResponseBody
    public ResponseEntity<String> processException(PaymentDetailsNotValidException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}