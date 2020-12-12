package edu.ohsu.cmp.htnu18app.model;

import org.hl7.fhir.r4.model.Patient;

public class PatientModel {
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
