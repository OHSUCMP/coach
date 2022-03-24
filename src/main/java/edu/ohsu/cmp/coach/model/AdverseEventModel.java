package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.app.Outcome;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.AdverseEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;

public class AdverseEventModel implements FHIRCompatible {

    private AdverseEvent sourceAdverseEvent;
    private Condition sourceCondition;

    private String system;
    private String code;
    private String description;
    private String outcome;         // should map with Outcome.fhirValue

    public AdverseEventModel(AdverseEvent adverseEvent) throws DataException {
        this(adverseEvent, null);
    }

    public AdverseEventModel(AdverseEvent adverseEvent, Condition sourceCondition) throws DataException {
        this.sourceAdverseEvent = adverseEvent;
        this.sourceCondition = sourceCondition;

        Coding c = adverseEvent.getEvent().getCodingFirstRep();
        this.system = c.getSystem();
        this.code = c.getCode();
        this.description = c.getDisplay();
        this.outcome = adverseEvent.getOutcome().getCodingFirstRep().getCode();
    }

    @Override
    public Bundle toBundle() {
        return FhirUtil.bundleResources(sourceAdverseEvent);
    }

    @JsonIgnore
    public AdverseEvent getSourceAdverseEvent() {
        return sourceAdverseEvent;
    }

    @JsonIgnore
    public Condition getSourceCondition() {
        return sourceCondition;
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
