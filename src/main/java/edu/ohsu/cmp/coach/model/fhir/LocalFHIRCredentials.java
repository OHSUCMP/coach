package edu.ohsu.cmp.coach.model.fhir;

public class LocalFHIRCredentials implements IFHIRCredentials {
    private String patientId;

    public LocalFHIRCredentials(String patientId) {
        this.patientId = patientId;
    }

    @Override
    public String getClientId() {
        return "local-client-id";
    }

    @Override
    public String getServerURL() {
        return "";
    }

    @Override
    public String getBearerToken() {
        return "lorem_ipsum_dolor_sit_amet";
    }

    @Override
    public String getPatientId() {
        return patientId;
    }

    @Override
    public String getUserId() {
        return patientId;
    }
}
