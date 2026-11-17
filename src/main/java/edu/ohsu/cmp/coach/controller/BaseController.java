package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.Audience;
import edu.ohsu.cmp.coach.service.AuditService;
import edu.ohsu.cmp.coach.service.ResourceService;
import edu.ohsu.cmp.coach.workspace.UserWorkspaceService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

public abstract class BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String AUDIENCE_COOKIE_NAME = "coachAudience";

    @Value("${application.name}")
    private String applicationName;

    @Value("${security.idle-timeout-seconds}")
    private Integer idleTimeoutSeconds;

    @Autowired
    protected UserWorkspaceService userWorkspaceService;

    @Autowired
    protected AuditService auditService;

    @Autowired
    protected Environment env;

    @Autowired
    protected FhirConfigManager fcm;

    @Autowired
    protected ResourceService resourceService;

    protected void setCommonViewComponents(Model model) {
        setCommonViewComponents(null, model);
    }

    protected void setCommonViewComponents(String sessionId, Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("idleTimeoutSeconds", idleTimeoutSeconds);

        if (sessionId != null) {
            try {
                model.addAttribute("siteSpecificResources", resourceService.getSiteSpecificResources(sessionId));
            } catch (SessionMissingException sme) {
                logger.warn("attempted to build site-specific resources for non-existent session {}", sessionId);
            }
        }
    }

    protected String getLaunchRedirect() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            return "/launch-patient";
        }

        HttpServletRequest request = requestAttributes.getRequest();
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return "/launch-patient";
        }

        return Arrays.stream(cookies)
                .filter(cookie -> StringUtils.equals(cookie.getName(), AUDIENCE_COOKIE_NAME))
                .map(Cookie::getValue)
                .filter(value -> StringUtils.equals(value, Audience.CARE_TEAM.name()))
                .findFirst()
                .map(value -> "/launch-provider")
                .orElse("/launch-patient");
    }
}
