package edu.ohsu.cmp.coach.exception;

public class MethodNotImplementedException extends RuntimeException {
    public MethodNotImplementedException() {
        super();
    }

    public MethodNotImplementedException(String message) {
        super(message);
    }

    public MethodNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodNotImplementedException(Throwable cause) {
        super(cause);
    }
}
