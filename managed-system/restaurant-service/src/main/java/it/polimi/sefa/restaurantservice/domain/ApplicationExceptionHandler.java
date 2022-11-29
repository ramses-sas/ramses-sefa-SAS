package it.polimi.sefa.restaurantservice.domain;

import it.polimi.sefa.restaurantservice.exceptions.ForcedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import it.polimi.sefa.restaurantservice.exceptions.RestaurantNotFoundException;

@ControllerAdvice

public class ApplicationExceptionHandler {
    @ExceptionHandler(RestaurantNotFoundException.class)
    @ResponseBody
    public ResponseEntity<String> processException(RestaurantNotFoundException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForcedException.class)
    @ResponseBody
    public ResponseEntity<String> processException(ForcedException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
