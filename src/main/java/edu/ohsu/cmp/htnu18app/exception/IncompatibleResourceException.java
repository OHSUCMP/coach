package edu.ohsu.cmp.htnu18app.exception;

public class IncompatibleResourceException extends Exception {
    public IncompatibleResourceException() {
        super();
    }

    public IncompatibleResourceException(String message) {
        super(message);
    }

    public IncompatibleResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompatibleResourceException(Throwable cause) {
        super(cause);
    }
}
