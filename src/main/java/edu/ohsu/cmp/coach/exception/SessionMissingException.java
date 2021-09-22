package edu.ohsu.cmp.coach.exception;

public class SessionMissingException extends RuntimeException {
    public SessionMissingException() {
        super();
    }

    public SessionMissingException(String message) {
        super(message);
    }

    public SessionMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionMissingException(Throwable cause) {
        super(cause);
    }
}
