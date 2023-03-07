package edu.ohsu.cmp.coach.exception;

public class OmronException extends Exception {
    public OmronException() {
    }

    public OmronException(String message) {
        super(message);
    }

    public OmronException(String message, Throwable cause) {
        super(message, cause);
    }

    public OmronException(Throwable cause) {
        super(cause);
    }

    public OmronException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
