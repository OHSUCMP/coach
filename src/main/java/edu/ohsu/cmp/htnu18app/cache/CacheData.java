package edu.ohsu.cmp.htnu18app.cache;

import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.model.recommendation.Audience;
import edu.ohsu.cmp.htnu18app.model.recommendation.Card;
import edu.ohsu.cmp.htnu18app.model.recommendation.Suggestion;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import java.util.*;

public class CacheData {
    private Audience audience;
    private FHIRCredentialsWithClient fhirCredentialsWithClient;
    private Long internalPatientId;
    private Patient patient;
    private Bundle observations;
    private Bundle conditions;
    private Bundle currentGoals;
    private Bundle medications;
    private Bundle adverseEvents;
    private Map<String, List<Card>> cards;
    private Map<String, Resource> resources;

    public CacheData(Audience audience, FHIRCredentialsWithClient fhirCredentialsWithClient, Long internalPatientId) {
        this.audience = audience;
        this.fhirCredentialsWithClient = fhirCredentialsWithClient;
        this.internalPatientId = internalPatientId;
        this.cards = new LinkedHashMap<String, List<Card>>();
        this.resources = new HashMap<String, Resource>();
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

    public Bundle getObservations() {
        return observations;
    }

    public void setObservations(Bundle observations) {
        this.observations = observations;
    }

    public Bundle getConditions() {
        return conditions;
    }

    public void setConditions(Bundle conditions) {
        this.conditions = conditions;
    }

    public Bundle getCurrentGoals() {
        return currentGoals;
    }

    public void setCurrentGoals(Bundle currentGoals) {
        this.currentGoals = currentGoals;
    }

    public Bundle getMedications() {
        return medications;
    }

    public void setMedications(Bundle medications) {
        this.medications = medications;
    }

    public Bundle getAdverseEvents() {
        return adverseEvents;
    }

    public void setAdverseEvents(Bundle adverseEvents) {
        this.adverseEvents = adverseEvents;
    }

    public boolean containsCards(String recommendationId) {
        return cards.containsKey(recommendationId);
    }

    public void setCards(String recommendationId, List<Card> list) {
        cards.put(recommendationId, list);
    }

    public List<Card> getCards(String recommendationId) {
        return cards.get(recommendationId);
    }

    public boolean deleteCards(String recommendationId) {
        if (cards.containsKey(recommendationId)) {
            cards.remove(recommendationId);
            return true;
        }
        return false;
    }

    public void deleteAllCards() {
        cards.clear();
    }

    /**
     * used to clear a particular Suggestion from the cache, by ID.  very useful for updating the cache in-place
     * after the user performs an action that should make that suggestion disappear
     * @param id
     * @return
     */
    public boolean deleteSuggestion(String id) {
        boolean rval = false;
        for (Map.Entry<String, List<Card>> entry : cards.entrySet()) {
            for (Card c : entry.getValue()) {
                if (c.getSuggestions() != null) {
                    Iterator<Suggestion> iter = c.getSuggestions().iterator();
                    while (iter.hasNext()) {
                        Suggestion s = iter.next();
                        if (s.getId() != null && s.getId().equals(id)) {    // ignore suggestions without IDs
                            iter.remove();
                            rval = true;
                        }
                    }
                }
            }
        }
        return rval;
    }

    public Resource getResource(String id) {
        return resources.get(id);
    }

    public void setResource(String id, Resource resource) {
        resources.put(id, resource);
    }
}
