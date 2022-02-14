package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class PatientModel {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String GENDER_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/patient-genderIdentity";

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

        if (p.hasExtension(GENDER_EXTENSION_URL)) {
            logger.debug("gender-identity extension found for Patient with id=" + id);

            Extension ext = p.getExtensionByUrl(GENDER_EXTENSION_URL);
            CodeableConcept cc = (CodeableConcept) ext.getValue();
            if (cc.hasCoding()) {
                Coding c = cc.getCodingFirstRep();

                if (c.hasDisplay()) {
                    logger.debug("setting gender=" + c.getDisplay() + " from extension Coding.display for Patient with id=" + id);
                    gender = c.getDisplay();

                } else if (c.hasCode()) {
                    logger.debug("setting gender=" + c.getCode() + " from extension Coding.code for Patient with id=" + id);
                    gender = c.getCode();

                }
            }

            if (gender == null && cc.hasText()) {
                logger.debug("setting gender=" + cc.getText() + " from extension CodeableConcept.text for Patient with id=" + id);
                gender = cc.getText();
            }
        }

        if (gender == null && p.hasGender()) {
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
