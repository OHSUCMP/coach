package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.service.AuditService;
import edu.ohsu.cmp.coach.service.ResourceService;
import edu.ohsu.cmp.coach.workspace.UserWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;

public abstract class BaseController {
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
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("idleTimeoutSeconds", idleTimeoutSeconds);
        model.addAttribute("siteSpecificResources", resourceService.getSiteSpecificResources());
    }
}
