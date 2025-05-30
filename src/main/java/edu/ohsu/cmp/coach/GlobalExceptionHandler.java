package edu.ohsu.cmp.coach;

import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AuditService auditService;

    @ExceptionHandler(Exception.class)
    public Object handleException(HttpSession session, HttpServletRequest request, HttpServletResponse response, Exception e) {
        if (e instanceof SessionMissingException) {
            logger.error("trapped " + e.getClass().getName() + " at " + request.getRequestURI() +
                    " for session " + session.getId());

        } else {
            logger.error("trapped " + e.getClass().getName() + " at " + request.getRequestURI() +
                    " for session " + session.getId() + " - " + e.getMessage() + " (enable DEBUG logging for stack trace)");
            logger.debug("stack trace: ", e);

            auditService.doAudit(session.getId(), AuditSeverity.ERROR, "application exception", "encountered " +
                    e.getClass().getSimpleName() + " at " + request.getRequestURI() + " - " + e.getMessage());
        }

        if ("get".equalsIgnoreCase(request.getMethod())) {
            return "error";

        } else {
            return new ResponseEntity<String>("the system encountered an internal error.  see logs for details.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}