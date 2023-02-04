package edu.ohsu.cmp.coach.model.fhir;

public interface IFHIRCredentials {
    String getClientId();
    String getServerURL();
    String getBearerToken();
    String getPatientId();
    String getUserId();
}
