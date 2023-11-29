package edu.ohsu.cmp.coach.exception;

import org.springframework.web.server.ResponseStatusException;

public class REDCapException extends ResponseStatusException {

    public REDCapException(int rawStatusCode, String reason, Throwable cause) {
        super(rawStatusCode, reason, cause);
    }
    
}
