package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.Patient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class PatientModel {
    private String id;
    private String name;
    private Long age;
    private String gender;

    public PatientModel(Patient p) {
        this.id = p.getId();
        this.name = p.getNameFirstRep().getNameAsSingleString();

        if (p.getBirthDate() != null) {
            LocalDate start = p.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate stop = LocalDate.now(ZoneId.systemDefault());
            age = ChronoUnit.YEARS.between(start, stop);
        }

        if (p.getGender() != null) {
            gender = p.getGender().getDisplay();
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
