package de.uni_trier.wi2.error;

public class DatabaseNotEmptyException extends Exception {
    public DatabaseNotEmptyException() {
        super("Database has to be empty for this test");
    }
}
