package de.uni_trier.wi2.error;

import org.apache.commons.lang.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
class NullArgumentAdvice {

    @ResponseBody
    @ExceptionHandler(NullArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String nullArgumentHandler(NullArgumentException ex) {
        return ex.getMessage();
    }
}