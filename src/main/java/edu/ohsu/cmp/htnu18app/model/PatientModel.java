package edu.ohsu.cmp.htnu18app.model;

import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatientModel {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String id;
    private String name;

    public PatientModel(Patient p) {
        this.id = p.getId();
        this.name = p.getNameFirstRep().getNameAsSingleString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
