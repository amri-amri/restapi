package de.uni_trier.wi2.error;

/**
 * Exception to be thrown if a given log is not valid against a given schema.
 */
public class XESnotValidException extends RuntimeException {
    public XESnotValidException(String log) {
        super(String.format(
                """
                        The given log does not validate against schema.
                        Log:
                        %s""",


                log));
    }
}
