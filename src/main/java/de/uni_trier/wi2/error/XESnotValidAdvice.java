package de.uni_trier.wi2.error;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class XESnotValidAdvice {

    @ResponseBody
    @ExceptionHandler(XESnotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String xesNotValidHandler(XESnotValidException ex) {
        return ex.getMessage();
    }

}
