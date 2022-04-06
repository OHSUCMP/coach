package edu.ohsu.cmp.coach.util;

import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;

import java.util.Arrays;
import java.util.List;

public class EncounterMatcher {
    private static final String DELIM = "\\s*,\\s*";

    private FhirConfigManager fcm;

    public EncounterMatcher(FhirConfigManager fcm) {
        this.fcm = fcm;
    }

    public boolean isAmbEncounter(Encounter e) {
        return e != null && encounterMatches(e,
                fcm.getEncounterAmbClassIn(),
                fcm.getEncounterAmbClassNotIn(),
                fcm.getEncounterAmbTypeIn(),
                fcm.getEncounterAmbTypeNotIn()
        );
    }

    public boolean isHomeHealthEncounter(Encounter e) {
        return e != null && encounterMatches(e,
                fcm.getEncounterHHClassIn(),
                fcm.getEncounterHHClassNotIn(),
                fcm.getEncounterHHTypeIn(),
                fcm.getEncounterHHTypeNotIn()
        );
    }

    private boolean encounterMatches(Encounter e, String classInStr, String classNotInStr, String typeInStr, String typeNotInStr) {
        List<String> classIn = StringUtils.isNotEmpty(classInStr) ?
                Arrays.asList(classInStr.split(DELIM)) :
                null;
        List<String> classNotIn = StringUtils.isNotEmpty(classNotInStr) ?
                Arrays.asList(classNotInStr.split(DELIM)) :
                null;
        List<String> typeIn = StringUtils.isNotEmpty(typeInStr) ?
                Arrays.asList(typeInStr.split(DELIM)) :
                null;
        List<String> typeNotIn = StringUtils.isNotEmpty(typeNotInStr) ?
                Arrays.asList(typeNotInStr.split(DELIM)) :
                null;

        boolean classMatches = (classIn == null    || ( e.hasClass_() && inClass(e.getClass_(), classIn))) &&
                               (classNotIn == null || ! e.hasClass_() || ! inClass(e.getClass_(), classNotIn));

        boolean typeMatches = (typeIn == null    || ( e.hasType() && inType(e.getType(), typeIn))) &&
                              (typeNotIn == null || ! e.hasType() || ! inType(e.getType(), typeNotIn));

        return classMatches && typeMatches;
    }

    private boolean inClass(Coding class_, List<String> list) {
        for (String s : list) {
            if (codingMatches(class_, s)) {
                return true;
            }
        }
        return false;
    }

    private boolean inType(List<CodeableConcept> types, List<String> list) {
        for (CodeableConcept type : types) {
            if (type.hasCoding()) {
                for (Coding coding : type.getCoding()) {
                    for (String s : list) {
                        if (codingMatches(coding, s)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean codingMatches(Coding coding, String s) {
        if (s.indexOf('|') > 0) { // system + code
            String[] arr = s.split("\\|");
            String system = arr[0];
            String code = arr[1];
            if (coding.hasSystem() && coding.getSystem().equals(system) && coding.hasCode() && coding.getCode().equals(code)) {
                return true;
            }
        } else {
            if (coding.hasCode() && coding.getCode().equals(s)) {
                return true;
            } else if (coding.hasDisplay() && coding.getDisplay().equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
