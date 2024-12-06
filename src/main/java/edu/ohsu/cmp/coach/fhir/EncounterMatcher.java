package edu.ohsu.cmp.coach.fhir;

import edu.ohsu.cmp.coach.util.FhirUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EncounterMatcher {
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

    public boolean isOfficeEncounter(Encounter e) {
        StringBuilder sb = doLogging && logger.isDebugEnabled() ? new StringBuilder() : null;
        if (sb != null) sb.append("isOffice?(").append(e.getId()).append("): ");

        boolean rval = e != null && encounterMatches(sb, e,
                fcm.getEncounterOfficeClassInCodings(),
                fcm.getEncounterOfficeClassNotInCodings(),
                fcm.getEncounterOfficeTypeInCodings(),
                fcm.getEncounterOfficeTypeNotInCodings()
        );

        if (sb != null) logger.debug(sb.toString());

        return rval;
    }

    public boolean isHomeEncounter(Encounter e) {
        StringBuilder sb = doLogging && logger.isDebugEnabled() ? new StringBuilder() : null;
        if (sb != null) sb.append("isHome?(").append(e.getId()).append("): ");

        boolean rval = e != null && encounterMatches(sb, e,
                fcm.getEncounterHomeClassInCodings(),
                fcm.getEncounterHomeClassNotInCodings(),
                fcm.getEncounterHomeTypeInCodings(),
                fcm.getEncounterHomeTypeNotInCodings()
        );

        if (sb != null) logger.debug(sb.toString());

        return rval;
    }

    private boolean encounterMatches(StringBuilder sb, Encounter e, List<Coding> classIn, List<Coding> classNotIn, List<Coding> typeIn, List<Coding> typeNotIn) {
        if (classIn != null && ! classIn.isEmpty()) {
            if (sb != null) sb.append("classInMatch? ");
            boolean match = e.hasClass_() && inClass(sb, e.getClass_(), classIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (class='")
                        .append(toCodingDebugString(e.getClass_()))
                        .append("')");
                return false;
            }
        }

        if (classNotIn != null && ! classNotIn.isEmpty()) {
            if (sb != null) sb.append("classNotInMatch? ");
            boolean match = ! e.hasClass_() || ! inClass(sb, e.getClass_(), classNotIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (class='")
                        .append(toCodingDebugString(e.getClass_()))
                        .append("')");
                return false;
            } else if (sb != null) {
                if ( ! e.hasClass_() ) {
                    sb.append("MATCH: (Encounter has no class) ");
                } else {
                    sb.append("MATCH: ")
                            .append(toCodingDebugString(e.getClass_()))
                            .append(" ");
                }
            }
        }

        if (typeIn != null && ! typeIn.isEmpty()) {
            if (sb != null) sb.append("typeInMatch? ");
            boolean match = e.hasType() && inType(sb, e.getType(), typeIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (type=")
                        .append(toCodeableConceptsDebugString(e.getType()))
                        .append(")");
                return false;
            }
        }

        if (typeNotIn != null && ! typeNotIn.isEmpty()) {
            if (sb != null) sb.append("typeNotInMatch? ");
            boolean match = ! e.hasType() || ! inType(sb, e.getType(), typeNotIn);
            if ( ! match ) {
                if (sb != null) sb.append("*NO* (type=")
                        .append(toCodeableConceptsDebugString(e.getType()))
                        .append(")");
                return false;
            } else if (sb != null) {
                if ( ! e.hasType() || e.getType().isEmpty() ) {
                    sb.append("MATCH: (Encounter has no type) ");
                } else {
                    sb.append("MATCH: ")
                            .append(toCodeableConceptsDebugString(e.getType()))
                            .append(" ");
                }
            }
        }

        if (sb != null) sb.append("*YES*");
        return true;
    }

    private boolean inClass(StringBuilder sb, Coding class_, List<Coding> list) {
        for (Coding c : list) {
            if (FhirUtil.codingMatches(class_, c, sb)) {
                return true;
            }
        }
        return false;
    }

    private boolean inType(StringBuilder sb, List<CodeableConcept> types, List<Coding> list) {
        for (CodeableConcept type : types) {
            if (type.hasCoding()) {
                for (Coding coding : type.getCoding()) {
                    for (Coding c : list) {
                        if (FhirUtil.codingMatches(coding, c, sb)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private String toCodeableConceptsDebugString(List<CodeableConcept> types) {
        if (types == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        for (CodeableConcept cc : types) {
            List<String> list = new ArrayList<>();
            for (Coding type : cc.getCoding()) {
                list.add(toCodingDebugString(type));
            }
            sb.append("[").append(String.join(", ", list)).append("]");
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
}
