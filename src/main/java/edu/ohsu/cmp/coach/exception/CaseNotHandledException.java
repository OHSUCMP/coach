package edu.ohsu.cmp.coach.exception;

public class CaseNotHandledException extends RuntimeException {
    public CaseNotHandledException() {
        super();
    }

    public CaseNotHandledException(String message) {
        super(message);
    }

    public CaseNotHandledException(String message, Throwable cause) {
        super(message, cause);
    }

    public CaseNotHandledException(Throwable cause) {
        super(cause);
    }
}
