package it.polimi.saefa.deliveryproxy3service.domain;

import it.polimi.saefa.deliveryproxy3service.exception.ForcedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice

public class ApplicationExceptionHandler {
    @ExceptionHandler(ForcedException.class)
    @ResponseBody
    public ResponseEntity<String> processException(ForcedException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
    }
}
