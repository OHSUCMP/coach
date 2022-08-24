package edu.ohsu.cmp.coach.model.fhir;

public class FHIRCredentials {
    private String clientId;
    private String serverURL;
    private String bearerToken;
    private String patientId;
    private String userId;

    public FHIRCredentials(String clientId, String serverURL, String bearerToken, String patientId, String userId) {
        this.clientId = clientId;
        this.serverURL = serverURL;
        this.bearerToken = bearerToken;
        this.patientId = patientId;
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
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
}
