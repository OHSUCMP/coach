package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.Outcome;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.AdverseEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;

public class AdverseEventModel implements FHIRCompatible {

    private AdverseEvent sourceAdverseEvent;
    private String system;
    private String code;
    private String description;
    private String outcome;         // should map with Outcome.fhirValue

    public AdverseEventModel(AdverseEvent adverseEvent) throws DataException {
        this.sourceAdverseEvent = adverseEvent;

        Coding c = adverseEvent.getEvent().getCodingFirstRep();
        this.system = c.getSystem();
        this.code = c.getCode();
        this.description = c.getDisplay();
        this.outcome = adverseEvent.getOutcome().getCodingFirstRep().getCode();
    }

    @Override
    public Bundle toBundle(String patientId, FhirConfigManager fcm) {
        // there is no scenario in which Adverse Events may be created that doesn't include a source FHIR resource
        // as such, we can just bundle it and send it back.  easy
        return FhirUtil.bundleResources(sourceAdverseEvent);
    }

    @JsonIgnore
    public AdverseEvent getSourceAdverseEvent() {
        return sourceAdverseEvent;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public boolean hasOutcome(Outcome o) {
        return o.getFhirValue().equals(outcome);
    }
}
