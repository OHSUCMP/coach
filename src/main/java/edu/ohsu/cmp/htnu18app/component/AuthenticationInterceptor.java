package edu.ohsu.cmp.htnu18app.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${launch.url}")
    private String launchUrl;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        if ( ! request.getRequestURL().toString().equals(launchUrl) ) {
//            String sessionId = request.getSession().getId();
//            logger.info("in AuthenticationInterceptor.preHandle() for sessionId=" + sessionId);
//
//            FHIRRegistry registry = FHIRRegistry.getInstance();
//            if ( ! registry.exists(sessionId) ) {
//                response.sendRedirect(launchUrl);
//                return false;
//            }
//        }

        return true;
    }
}
