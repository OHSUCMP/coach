package edu.ohsu.cmp.coach.fhir;

import edu.ohsu.cmp.coach.util.FhirUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EncounterMatcher {
    private static final String DELIM = "\\s*,\\s*";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private FhirConfigManager fcm;
    private boolean doLogging;

    public EncounterMatcher(FhirConfigManager fcm) {
        this(fcm, false);
    }

    public EncounterMatcher(FhirConfigManager fcm, boolean doLogging) {
        this.fcm = fcm;
        this.doLogging = doLogging;
    }

    public boolean isAmbEncounter(Encounter e) {
        StringBuilder sb = doLogging && logger.isDebugEnabled() ? new StringBuilder() : null;
        if (sb != null) sb.append("isAmb?(").append(e.getId()).append("): ");

        boolean rval = e != null && encounterMatches(sb, e,
                fcm.getEncounterAmbClassIn(),
                fcm.getEncounterAmbClassNotIn(),
                fcm.getEncounterAmbTypeIn(),
                fcm.getEncounterAmbTypeNotIn()
        );

        if (sb != null) logger.debug(sb.toString());

        return rval;
    }

    public boolean isHomeHealthEncounter(Encounter e) {
        StringBuilder sb = doLogging && logger.isDebugEnabled() ? new StringBuilder() : null;
        if (sb != null) sb.append("isHH?(").append(e.getId()).append("): ");

        boolean rval = e != null && encounterMatches(sb, e,
                fcm.getEncounterHHClassIn(),
                fcm.getEncounterHHClassNotIn(),
                fcm.getEncounterHHTypeIn(),
                fcm.getEncounterHHTypeNotIn()
        );

        if (sb != null) logger.debug(sb.toString());

        return rval;
    }

    private boolean encounterMatches(StringBuilder sb, Encounter e, String classInStr, String classNotInStr, String typeInStr, String typeNotInStr) {
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

        if (classIn != null) {
            if (sb != null) sb.append("classInMatch? ");
            boolean match = e.hasClass_() && inClass(sb, e.getClass_(), classIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (class='")
                        .append(toCodingDebugString(e.getClass_()))
                        .append("')");
                return false;
            }
        }

        if (classNotIn != null) {
            if (sb != null) sb.append("classNotInMatch? ");
            boolean match = ! e.hasClass_() || ! inClass(sb, e.getClass_(), classNotIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (class='")
                        .append(FhirUtil.toCodingString(e.getClass_()))
                        .append("')");
                return false;
            }
        }

        if (typeIn != null) {
            if (sb != null) sb.append("typeInMatch? ");
            boolean match = e.hasType() && inType(sb, e.getType(), typeIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (type=")
                        .append(typesToString(e.getType()))
                        .append(")");
                return false;
            }
        }

        if (typeNotIn != null) {
            if (sb != null) sb.append("typeNotInMatch? ");
            boolean match = ! e.hasType() || ! inType(sb, e.getType(), typeNotIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (type=")
                        .append(typesToString(e.getType()))
                        .append(")");
                return false;
            }
        }

        if (sb != null) sb.append("*YES*");
        return true;
    }

    private String typesToString(List<CodeableConcept> types) {
        if (types == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        for (CodeableConcept cc : types) {
            sb.append("[");
            for (Coding type : cc.getCoding()) {
                sb.append(toCodingDebugString(type)).append(",");
            }
            sb.append("]");
        }
        sb.append(">");
        return sb.toString();
    }

    private String toCodingDebugString(Coding coding) {
        if (coding == null) return "null";
        List<String> parts = new ArrayList<>();
        if (coding.hasSystem()) parts.add("system=" + coding.getSystem());
        if (coding.hasCode()) parts.add("code=" + coding.getCode());
        if (coding.hasDisplay()) parts.add("display=" + coding.getDisplay());
        return "{" + StringUtils.join(parts, "|") + "}";
    }

    private boolean inClass(StringBuilder sb, Coding class_, List<String> list) {
        for (String s : list) {
            if (codingMatches(sb, class_, s)) {
                return true;
            }
        }
        return false;
    }

    private boolean inType(StringBuilder sb, List<CodeableConcept> types, List<String> list) {
        for (CodeableConcept type : types) {
            if (type.hasCoding()) {
                for (Coding coding : type.getCoding()) {
                    for (String s : list) {
                        if (codingMatches(sb, coding, s)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean codingMatches(StringBuilder sb, Coding coding, String s) {
        if (s.indexOf('|') > 0) { // system + code
            String[] arr = s.split("\\|");
            String system = arr[0];
            String code = arr[1];
            if (coding.hasSystem() && coding.getSystem().equals(system) && coding.hasCode() && coding.getCode().equals(code)) {
                if (sb != null) sb.append("MATCH (codesystem): '").append(s).append("' ");
                return true;
            }
        } else {
            if (coding.hasCode() && coding.getCode().equals(s)) {
                if (sb != null) sb.append("MATCH (code): '").append(s).append("' ");
                return true;
            } else if (coding.hasDisplay() && coding.getDisplay().equalsIgnoreCase(s)) {
                if (sb != null) sb.append("MATCH (display): '").append(s).append("' ");
                return true;
            }
        }
        return false;
    }
}
