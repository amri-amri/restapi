package de.uni_trier.wi2.error;

import org.apache.commons.lang.NullArgumentException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class NullArgumentAdvice {

    @ResponseBody
    @ExceptionHandler(NullArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String nullArgumentHandler(NullArgumentException ex) {
        return ex.getMessage();
    }
}