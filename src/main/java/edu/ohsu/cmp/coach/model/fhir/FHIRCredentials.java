package edu.ohsu.cmp.coach.model.fhir;

import org.apache.commons.lang3.StringUtils;

public class FHIRCredentials {
    private String serverURL;
    private String bearerToken;
    private String patientId;
    private String userId;
    private String jwt;

    public FHIRCredentials(String serverURL, String bearerToken, String patientId, String userId, String jwt) {
        this.serverURL = serverURL;
        this.bearerToken = bearerToken;
        this.patientId = patientId;
        this.userId = userId;
        this.jwt = StringUtils.isNotBlank(jwt) ? jwt : null;
    }

    public String getServerURL() {
        return serverURL;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean hasJwt() {
        return jwt != null;
    }

    public String getJwt() {
        return jwt;
    }
}
