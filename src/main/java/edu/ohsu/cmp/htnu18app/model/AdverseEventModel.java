package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.exception.IncompatibleResourceException;
import org.hl7.fhir.r4.model.AdverseEvent;
import org.hl7.fhir.r4.model.Coding;

public class AdverseEventModel {
    private String system;
    private String code;
    private String description;
    private String outcome;

    public AdverseEventModel(AdverseEvent adverseEvent) throws DataException, IncompatibleResourceException {
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
}
