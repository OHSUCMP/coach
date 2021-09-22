package edu.ohsu.cmp.coach.model.fhir;

public class FHIRCredentials {
    private String serverURL;
    private String bearerToken;
    private String patientId;
    private String userId;

    public FHIRCredentials(String serverURL, String bearerToken, String patientId, String userId) {
        this.serverURL = serverURL;
        this.bearerToken = bearerToken;
        this.patientId = patientId;
        this.userId = userId;
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
