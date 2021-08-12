package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.exception.IncompatibleResourceException;
import edu.ohsu.cmp.htnu18app.exception.MethodNotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AdverseEvent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;

public class AdverseEventModel {
    private String system;
    private String code;
    private String description;

    public AdverseEventModel(IBaseResource resource) throws DataException, IncompatibleResourceException {
        if (resource instanceof AdverseEvent) {
            createFromAdverseEvent((AdverseEvent) resource);

        } else if (resource instanceof Condition) {
            createFromCondition((Condition) resource);

        } else {
            throw new IncompatibleResourceException("cannot create AdverseEventModel from " + resource.getClass().getName());
        }
    }

    private void createFromAdverseEvent(AdverseEvent ae) {
        // stubbing this out in case we want to use it maybe at some point?
        throw new MethodNotImplementedException();
    }

    private void createFromCondition(Condition c) {
        Coding cfr = c.getCode().getCodingFirstRep();
        this.system = cfr.getSystem();
        this.code = cfr.getCode();
        this.description = cfr.getDisplay();
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
