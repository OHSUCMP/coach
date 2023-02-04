package edu.ohsu.cmp.coach.model.fhir;

public class FHIRCredentials implements IFHIRCredentials {
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

    @Override
    public String toString() {
        return "FHIRCredentials{" +
                "clientId='" + clientId + '\'' +
                ", serverURL='" + serverURL + '\'' +
                ", bearerToken='" + bearerToken + '\'' +
                ", patientId='" + patientId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getServerURL() {
        return serverURL;
    }

    @Override
    public String getBearerToken() {
        return bearerToken;
    }

    @Override
    public String getPatientId() {
        return patientId;
    }

    @Override
    public String getUserId() {
        return userId;
    }
}
