package edu.ohsu.cmp.coach.exception;

public class ScopeException extends Exception {
    public ScopeException() {
        super();
    }

    public ScopeException(String message) {
        super(message);
    }

    public ScopeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScopeException(Throwable cause) {
        super(cause);
    }
}
