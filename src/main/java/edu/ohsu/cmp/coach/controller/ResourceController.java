package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.model.AuditSeverity;
import edu.ohsu.cmp.coach.model.SiteSpecificResource;
import edu.ohsu.cmp.coach.model.redcap.RandomizationGroup;
import edu.ohsu.cmp.coach.service.ResourceService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

// These pages may be linked to in the recommendations. Don't change the URL.
@Controller
@RequestMapping("/resources")
public class ResourceController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String WELCOME_VIDEO_ID_CONTROL = "m0751n7glxU";
    private static final String WELCOME_VIDEO_ID_INTERVENTION = "AQGLdXV9ZTE";

    @Autowired
    private ResourceService resourceService;

    @GetMapping("/faq")
    public String faq(HttpSession session, Model model) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: faq");

        return "faq";
    }

    @GetMapping("/symptoms-911")
    public String symptoms(HttpSession session, Model model) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: symptoms-911");

        return "symptoms";
    }

    @GetMapping("/side-effects")
    public String sideEffects(HttpSession session, Model model) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: side-effects");

        return "side-effects";
    }

    @GetMapping("/welcome-video")
    public String welcomeVideo(HttpSession session, Model model) {
        setCommonViewComponents(model);
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        String videoId = workspace.getActiveRandomizationGroup() == RandomizationGroup.ENHANCED ?
                WELCOME_VIDEO_ID_INTERVENTION :
                WELCOME_VIDEO_ID_CONTROL;
        model.addAttribute("videoId", videoId);

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: welcome-video");

        return "embedded-video";
    }

    @GetMapping("/pdf/{filename}")
    public ResponseEntity<InputStreamResource> getPdf(HttpSession session, @PathVariable("filename") String filename) {
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        String path = workspace.getActiveRandomizationGroup() == RandomizationGroup.ENHANCED ?
                "intervention" :
                "control";
        String resName = "static/pdf/" + path + "/" + filename;
        ClassLoader classLoader = ResourceController.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(resName);
        if (inputStream != null) {
            InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.inline().filename(filename).build());
            headers.setContentType(MediaType.APPLICATION_PDF);
            return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
        } else {
            logger.warn("requested resource does not exist: " + resName);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/risks-of-hypertension-study-results")
    public String risksOfHypertensionStudyResults(HttpSession session, Model model) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);
        model.addAttribute("pdfUrl", "/resources/pdf/Risks_of_Hypertension_Study_Results.pdf");

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: risks-of-hypertension-study-results");

        return "embedded-pdf";
    }

    @GetMapping("/coach-written-instructions")
    public String coachWrittenInstructions(HttpSession session, Model model) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);
        model.addAttribute("pdfUrl", "/resources/pdf/COACH_Written_Instructions.pdf");

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: coach-written-instructions");

        return "embedded-pdf";
    }

    @GetMapping("/omron-instructions")
    public String omronInstructions(HttpSession session, Model model) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);
        model.addAttribute("pdfUrl", "/resources/pdf/OMRON_Instructions.pdf");

        auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed resources: omron-instructions");

        return "embedded-pdf";
    }

    @GetMapping("/site-pdf/{key}")
    public String getSiteSpecificResource(HttpSession session, Model model, @PathVariable("key") String key) {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        setCommonViewComponents(model);
        model.addAttribute("pdfUrl", "/resources/site-pdf-raw/" + key);
        return "embedded-pdf";
    }

    @GetMapping("/site-pdf-raw/{key}")
    public ResponseEntity<InputStreamResource> getSitePdf(HttpSession session, @PathVariable("key") String key) throws FileNotFoundException {
        userWorkspaceService.get(session.getId());  // don't need it, but we do want to blow out with an error if the user's session doesn't exist
        SiteSpecificResource resource = resourceService.getSiteSpecificResource(key);
        if (resource != null) {
            FileInputStream inputStream = new FileInputStream(resource.getFile());
            InputStreamResource inputStreamResource = new InputStreamResource(inputStream);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.inline().filename(resource.getFilename()).build());
            headers.setContentType(MediaType.APPLICATION_PDF);

            auditService.doAudit(session.getId(), AuditSeverity.INFO, "viewed site-specific resources: " + resource.getName());

            return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
        } else {
            logger.warn("requested site-specific resource key does not exist: " + key);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
