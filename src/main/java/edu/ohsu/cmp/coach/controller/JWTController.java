package edu.ohsu.cmp.coach.controller;

import com.google.gson.Gson;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.model.fhir.jwt.WebKeySet;
import edu.ohsu.cmp.coach.service.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/jwt")
public class JWTController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private JWTService jwtService;

    @GetMapping(value="jwks", produces="application/json")
    public ResponseEntity<String> getJSONWebKeySet() throws ConfigurationException {
        WebKeySet webKeySet = jwtService.getWebKeySet();

        if (webKeySet == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Gson gson = new Gson();
        String json = gson.toJson(webKeySet, WebKeySet.class);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }
}