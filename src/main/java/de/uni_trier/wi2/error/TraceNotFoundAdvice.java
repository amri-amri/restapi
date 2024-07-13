package de.uni_trier.wi2.error;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
class TraceNotFoundAdvice {

    @ResponseBody
    @ExceptionHandler(TraceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String traceNotFoundHandler(TraceNotFoundException ex) {
        return ex.getMessage();
    }
}