package restapi.error;

/**
 * Exception to be thrown if a trace with given id cannot be found in the database.
 */
public class TraceNotFoundException extends RuntimeException{
    public TraceNotFoundException(String id){
        super(String.format("Could not find trace with id '%s'", id));
    }
}
