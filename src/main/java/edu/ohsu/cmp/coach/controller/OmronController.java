package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.exception.SessionMissingException;
import edu.ohsu.cmp.coach.model.MyOmronTokenData;
import edu.ohsu.cmp.coach.model.omron.AccessTokenResponse;
import edu.ohsu.cmp.coach.model.omron.OmronNotifyModel;
import edu.ohsu.cmp.coach.model.omron.OmronStatusData;
import edu.ohsu.cmp.coach.service.OmronService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/omron")
public class OmronController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OmronService omronService;

    @GetMapping("oauth")
    public String oauth(HttpSession session,
                        @RequestParam(required = false) String code,
                        @RequestParam(required = false) String error) throws Exception {

        UserWorkspace workspace = userWorkspaceService.get(session.getId());

        // if code exists, then success; use code to obtain a token
        // else if error, then failure; display error in UI and abort

        if (StringUtils.isNotBlank(code)) {
            try {
                AccessTokenResponse accessTokenResponse = omronService.requestAccessToken(code);
                if (accessTokenResponse != null) {
                    logger.debug("got Omron access token: " + accessTokenResponse.getAccessToken());
                    workspace.setOmronTokenData(new MyOmronTokenData(accessTokenResponse));
                }
            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " getting Omron access token - " + e.getMessage(), e);
                throw e;
            }

            try {
                omronService.scheduleAccessTokenRefresh(session.getId());
            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " scheduling Omron access token refresh - " + e.getMessage(), e);
                throw e;
            }

            try {
                workspace.initiateSynchronousOmronUpdate();
            } catch (Exception e) {
                logger.error("caught " + e.getClass().getName() + " synchronizing with Omron - " + e.getMessage(), e);
                throw e;
            }

        } else if (StringUtils.isNotBlank(error)) {
            logger.error("caught error during Omron authorization: " + error);
        }

        return "redirect:/";
    }

    @PostMapping(value = "notify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> notify(@RequestBody OmronNotifyModel notification) {
        try {
            UserWorkspace workspace = userWorkspaceService.getByOmronUserId(notification.getId());

            // note : this endpoint will be called by Omron when an authenticated user has new data to pull
            //        as such, this endpoint does not connect to any existing user session.  we need to look up
            //        the user by their id

            logger.info("received Omron notification: {}", notification);

            // id = the id of the user who performed the upload.  received into id_token on initial user authorization

            workspace.initiateSynchronousOmronUpdate();

        } catch (SessionMissingException sme) {
            logger.debug("notify: no workspace found for user with Omron id=" + notification.getId());
        }

        return new ResponseEntity<>(HttpStatus.OK);         // returns only OK status, no body
    }

    @PostMapping("status")
    public ResponseEntity<OmronStatusData> getStatus(HttpSession session) {
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        return new ResponseEntity<>(workspace.getOmronSynchronizationStatus(), HttpStatus.OK);
    }
}
