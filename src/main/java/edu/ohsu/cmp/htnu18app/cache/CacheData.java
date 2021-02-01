package edu.ohsu.cmp.htnu18app.cache;

import edu.ohsu.cmp.htnu18app.cqfruler.model.Card;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CacheData {
    private FHIRCredentialsWithClient fhirCredentialsWithClient;
    private Long internalPatientId;
    private Patient patient;
    private Bundle bpList;
    private Map<String, List<Card>> cards;

    public CacheData(FHIRCredentialsWithClient fhirCredentialsWithClient, Long internalPatientId) {
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.internalPatientId = internalPatientId;
        this.cards = new LinkedHashMap<String, List<Card>>();
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

    public Bundle getBpList() {
        return bpList;
    }

    public void setBpList(Bundle bpList) {
        this.bpList = bpList;
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
