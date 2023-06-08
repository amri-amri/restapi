package restapi.error;

import restapi.service.DatabaseService;

/**
 * Exception to be thrown if a given log is not valid against a given schema.
 */
public class XESnotValidException extends RuntimeException{
    public XESnotValidException(String log, DatabaseService.XSD_SCHEMATA.XSD xsd){
        super(String.format(
                """
                        The given log does not validate against schema.
                        Schema: %s
                        Log:
                        %s""",

                xsd.SOURCE,
                log));
    }
}
