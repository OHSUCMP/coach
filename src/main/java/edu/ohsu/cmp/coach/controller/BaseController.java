package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.service.AuditService;
import edu.ohsu.cmp.coach.service.ResourceService;
import edu.ohsu.cmp.coach.workspace.UserWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;

public abstract class BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
                logger.warn("attempted to build site-specific resources for non-existent session " + sessionId);
            }
        }
    }
}
