package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.model.MyOmronTokenData;
import edu.ohsu.cmp.coach.model.omron.AccessTokenResponse;
import edu.ohsu.cmp.coach.service.OmronService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/omron")
public class OmronController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // todo : expose endpoints for Omron authentication and API integration here

    @Autowired
    private OmronService omronService;

    @GetMapping(value = {"", "/"})
    public String view(HttpSession session, Model model) throws DataException {
        model.addAttribute("authorizationRequestUrl", omronService.getAuthorizationRequestUrl());
        return "omron";
    }

    @GetMapping("oauth")
    public String oauth(HttpSession session,
                        @RequestParam(required = false) String code,
                        @RequestParam(required = false) String error) {

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
                throw new RuntimeException(e);
            }

        } else if (StringUtils.isNotBlank(error)) {
            logger.error("caught error during Omron authorization: " + error);
        }

        return "omron-oauth";
    }

    @PostMapping("notify")
    public ResponseEntity<String> notify(HttpSession session,
                                         String id,
                                         String timestamp) {

        // note : this endpoint will be called by Omron when an authenticated user has new data to pull
        //        as such, this endpoint does not connect to any existing user session.  we need to look up
        //        the user by their id

        logger.info("received notification for session " + session.getId() + ": id=" + id + ", timestamp=" + timestamp);

        // id = the id of the user who performed the upload.  received into id_token on initial user authorization

        // todo : get recent blood pressure measurements

        return new ResponseEntity<>(HttpStatus.OK);         // returns only OK status, no body
    }
}
