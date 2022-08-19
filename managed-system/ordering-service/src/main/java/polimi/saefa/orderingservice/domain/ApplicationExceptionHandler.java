package polimi.saefa.orderingservice.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import polimi.saefa.orderingservice.exceptions.*;
import polimi.saefa.orderingservice.restapi.ConfirmOrderResponse;

@ControllerAdvice
public class ApplicationExceptionHandler {

    @Autowired
    OrderingService orderingService;

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

    @ExceptionHandler(ConfirmOrderException.class)
    @ResponseBody
    public ResponseEntity<String> processException(ConfirmOrderException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(PaymentNotAvailableException.class)
    @ResponseBody
    public ConfirmOrderResponse processException(PaymentNotAvailableException e) {
        return new ConfirmOrderResponse(false, true, null);
    }

    @ExceptionHandler(DeliveryNotAvailableException.class)
    @ResponseBody
    public ConfirmOrderResponse processException(DeliveryNotAvailableException e) {
        return new ConfirmOrderResponse(false, orderingService.orderRequiresCashPayment(e.getCartId()), true);
    }
}