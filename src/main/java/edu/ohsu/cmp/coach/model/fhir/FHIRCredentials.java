package edu.ohsu.cmp.coach.model.fhir;

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
        this.jwt = jwt;
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

    public String getJwt() {
        return jwt;
    }
}
