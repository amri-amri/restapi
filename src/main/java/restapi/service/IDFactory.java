package restapi.service;

/**
 * Interface for ID creation.
 */
public interface IDFactory {
    String nextID();
    void reset();
}
