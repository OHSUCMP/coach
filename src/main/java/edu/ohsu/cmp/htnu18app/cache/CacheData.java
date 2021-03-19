package edu.ohsu.cmp.htnu18app.cache;

import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.model.recommendation.Audience;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CacheData {
    private Audience audience;
    private FHIRCredentialsWithClient fhirCredentialsWithClient;
    private Long internalPatientId;
    private Patient patient;
    private Bundle bloodPressureObservations;
    private Bundle medicationStatements;
    private Map<String, List<Card>> cards;

    public CacheData(Audience audience, FHIRCredentialsWithClient fhirCredentialsWithClient, Long internalPatientId) {
        this.audience = audience;
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.internalPatientId = internalPatientId;
        this.cards = new LinkedHashMap<String, List<Card>>();
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public FHIRCredentialsWithClient getFhirCredentialsWithClient() {
        return fhirCredentialsWithClient;
    }

    public void setFhirCredentialsWithClient(FHIRCredentialsWithClient fhirCredentialsWithClient) {
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
    }

    public Long getInternalPatientId() {
        return internalPatientId;
    }

    public void setInternalPatientId(Long internalPatientId) {
        this.internalPatientId = internalPatientId;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Bundle getBloodPressureObservations() {
        return bloodPressureObservations;
    }

    public void setBloodPressureObservations(Bundle bloodPressureObservations) {
        this.bloodPressureObservations = bloodPressureObservations;
    }

    public Bundle getMedicationStatements() {
        return medicationStatements;
    }

    public void setMedicationStatements(Bundle medicationStatements) {
        this.medicationStatements = medicationStatements;
    }

    public boolean containsCards(String hookId) {
        return cards.containsKey(hookId);
    }

    public void setCards(String hookId, List<Card> list) {
        cards.put(hookId, list);
    }

    public List<Card> getCards(String hookId) {
        return cards.get(hookId);
    }

    public boolean deleteCards(String hookId) {
        if (cards.containsKey(hookId)) {
            cards.remove(hookId);
            return true;
        }
        return false;
    }

    public void deleteAllCards() {
        cards.clear();
    }
}
