package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.entity.app.Outcome;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import org.hl7.fhir.r4.model.AdverseEvent;
import org.hl7.fhir.r4.model.Coding;

public class AdverseEventModel {

    private String system;
    private String code;
    private String description;
    private String outcome;         // should map with Outcome.fhirValue

    public AdverseEventModel(AdverseEvent adverseEvent) throws DataException {
        Coding c = adverseEvent.getEvent().getCodingFirstRep();
        this.system = c.getSystem();
        this.code = c.getCode();
        this.description = c.getDisplay();
        this.outcome = adverseEvent.getOutcome().getCodingFirstRep().getCode();
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
