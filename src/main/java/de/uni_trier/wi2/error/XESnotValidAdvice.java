package de.uni_trier.wi2.error;

import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
public class XESnotValidAdvice {

    @ResponseBody
    @ExceptionHandler(XESnotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String xesNotValidHandler(XESnotValidException ex) {
        return ex.getMessage();
    }

}
