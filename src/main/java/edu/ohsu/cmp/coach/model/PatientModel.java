package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class PatientModel {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String GENDER_EXTENSION_URL = "http://hl7.org/fhir/StructureDefinition/patient-genderIdentity";

    private Patient sourcePatient;

    private String id;
    private String name;
    private Long age;
    private String gender;

    public PatientModel(Patient p) {
        this.sourcePatient = p;
        this.id = p.getId();

        String officialName = null;
        String usualName = null;
        String defaultName = null;
        if (p.hasName()) {
            defaultName = p.getNameFirstRep().getNameAsSingleString();
            for (HumanName hn : p.getName()) {
                if (officialName == null && hn.getUse() == HumanName.NameUse.OFFICIAL) {
                    officialName = buildName(hn);
                } else if (usualName == null && hn.getUse() == HumanName.NameUse.USUAL) {
                    usualName = buildName(hn);
                }
            }
        }
        if      (usualName != null)     this.name = usualName;
        else if (officialName != null)  this.name = officialName;
        else {
            logger.warn("no USUAL or OFFICIAL name for patient " + p.getId() + " - using default");
            this.name = defaultName;
        }

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

    @JsonIgnore
    public Patient getSourcePatient() {
        return sourcePatient;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

///////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private String buildName(HumanName hn) {
        if (hn != null) {
            if (hn.hasText()) {
                // if the name element has text, just use that
                return hn.getText();

            } else if (hn.hasFamily() && hn.hasGiven()) {
                // otherwise, construct it from parts, providing those exist
                return hn.getGivenAsSingleString() + " " + hn.getFamily();
            }
        }
        return null;
    }
}
