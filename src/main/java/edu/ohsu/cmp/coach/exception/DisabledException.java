package edu.ohsu.cmp.coach.exception;

public class DisabledException extends ConfigurationException {
    public DisabledException() {
    }

    public DisabledException(String message) {
        super(message);
    }

    public DisabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public DisabledException(Throwable cause) {
        super(cause);
    }
}
