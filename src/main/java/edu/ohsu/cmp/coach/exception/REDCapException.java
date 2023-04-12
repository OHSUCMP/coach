package edu.ohsu.cmp.coach.exception;

public class REDCapException extends Exception {
    public REDCapException() {
        super();
    }

    public REDCapException(String message) {
        super(message);
    }

    public REDCapException(String message, Throwable cause) {
        super(message, cause);
    }

    public REDCapException(Throwable cause) {
        super(cause);
    }

    protected REDCapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
