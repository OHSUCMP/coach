package edu.ohsu.cmp.htnu18app.cache;

import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

public class CacheData {
    private FHIRCredentialsWithClient fhirCredentialsWithClient;
    private int internalPatientId;
    private Patient patient;
    private Bundle bpList;

    public CacheData(FHIRCredentialsWithClient fhirCredentialsWithClient, int internalPatientId) {
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.internalPatientId = internalPatientId;
    }

    public FHIRCredentialsWithClient getFhirCredentialsWithClient() {
        return fhirCredentialsWithClient;
    }

    public void setFhirCredentialsWithClient(FHIRCredentialsWithClient fhirCredentialsWithClient) {
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
    }

    public int getInternalPatientId() {
        return internalPatientId;
    }

    public void setInternalPatientId(int internalPatientId) {
        this.internalPatientId = internalPatientId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Bundle getBpList() {
        return bpList;
    }

    public void setBpList(Bundle bpList) {
        this.bpList = bpList;
    }
}
