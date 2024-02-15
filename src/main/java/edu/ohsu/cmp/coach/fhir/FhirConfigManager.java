package edu.ohsu.cmp.coach.fhir;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@PropertySource("${fhirconfig.file}")
public class FhirConfigManager {
    private static final String ENCOUNTER_LOOKBACK_PERIOD = "2y";
    private static final Coding BP_PANEL_COMMON_CODING = new Coding("http://loinc.org", "55284-4", "Blood pressure systolic and diastolic");
    private static final Coding BP_SYSTOLIC_COMMON_CODING = new Coding("http://loinc.org", "8480-6", "Systolic blood pressure");
    private static final Coding BP_DIASTOLIC_COMMON_CODING = new Coding("http://loinc.org", "8462-4", "Diastolic blood pressure");
    private static final String BP_VALUE_CODE = "mm[Hg]";
    private static final String BP_VALUE_SYSTEM = "http://unitsofmeasure.org";
    private static final String BP_VALUE_UNIT = "mmHg";
    private static final String BP_LOOKBACK_PERIOD = "2y";
    private static final Coding PULSE_COMMON_CODING = new Coding("http://loinc.org", "8867-4", "Heart rate");
    private static final String PULSE_VALUE_CODE = "/min";
    private static final String PULSE_VALUE_SYSTEM = "http://unitsofmeasure.org";
    private static final String PULSE_VALUE_UNIT = "beats/minute";
    private static final String PULSE_LOOKBACK_PERIOD = "2y";
    private static final Coding PROTOCOL_CODING = new Coding("http://loinc.org", "9855-8", "Blood pressure special circumstances");
    private static final Coding PROTOCOL_ANSWER_CODING = new Coding("http://loinc.org", "LA46-8", "Other\\, Specify");
    private static final String PROTOCOL_LOOKBACK_PERIOD = "2y";
    private static final Coding BMI_CODING = new Coding("http://loinc.org", "39156-5", "Body mass index");
    private static final String BMI_LOOKBACK_PERIOD = "2y";
    private static final Coding SMOKING_CODING = new Coding("http://loinc.org", "72166-2", "Tobacco smoking status");
    private static final String SMOKING_LOOKBACK_PERIOD = "5y";
    private static final Coding DRINKS_CODING = new Coding("http://loinc.org", "11287-0", "Alcoholic drinks/drinking D Reported");
    private static final String DRINKS_LOOKBACK_PERIOD = "5y";
    private static final Coding PROCEDURE_COUNSELING_CODING = new Coding("http://snomed.info/sct", "409063005", "Counseling (procedure)");


    @Autowired
    private Environment env;

    private Coding encounterClassOfficeCoding = null;
    private Coding encounterClassHomeCoding = null;
    private List<Coding> encounterOfficeClassInCodings = null;
    private List<Coding> encounterOfficeClassNotInCodings = null;
    private List<Coding> encounterOfficeTypeInCodings = null;
    private List<Coding> encounterOfficeTypeNotInCodings = null;
    private List<Coding> encounterHomeClassInCodings = null;
    private List<Coding> encounterHomeClassNotInCodings = null;
    private List<Coding> encounterHomeTypeInCodings = null;
    private List<Coding> encounterHomeTypeNotInCodings = null;

    private List<Coding> bpOfficeCodings = null;
    private List<Coding> bpHomeCodings = null;
    private List<Coding> bpPanelCustomCodings = null;
    private List<Coding> bpSystolicCustomCodings = null;
    private List<Coding> bpDiastolicCustomCodings = null;

    @Value("${bp.limit}")           private String bpLimit;

    private List<Coding> pulseCustomCodings = null;

    @Value("${protocol.answer.yes}")        private String protocolAnswerYes;
    @Value("${protocol.answer.no}")         private String protocolAnswerNo;

    private List<Coding> serviceRequestOrderBPGoalCodings = null;
    private Pattern serviceRequestOrderBPGoalNoteSystolicRegex = null;
    private Pattern serviceRequestOrderBPGoalNoteDiastolicRegex = null;

    public Coding getEncounterClassOfficeCoding() {   // ambulatory class to attach to crafted office visit encounters
        if (encounterClassOfficeCoding == null) {
            encounterClassOfficeCoding = buildCoding(env.getProperty("encounter.class.office.coding"));
        }
        return encounterClassOfficeCoding;
    }

    public Coding getEncounterClassHomeCoding() {   // ambulatory class to attach to crafted home encounters
        if (encounterClassHomeCoding == null) {
            encounterClassHomeCoding = buildCoding(env.getProperty("encounter.class.home.coding"));
        }
        return encounterClassHomeCoding;
    }

    public List<Coding> getEncounterOfficeClassInCodings() {  // for matching incoming ambulatory Encounters
        if (encounterOfficeClassInCodings == null) {
            encounterOfficeClassInCodings = buildCodings(env.getProperty("encounter.office.class.in.codings"));
        }
        return encounterOfficeClassInCodings;
    }

    public List<Coding> getEncounterOfficeClassNotInCodings() {  // for matching incoming ambulatory Encounters
        if (encounterOfficeClassNotInCodings == null) {
            encounterOfficeClassNotInCodings = buildCodings(env.getProperty("encounter.office.class.not-in.codings"));
        }
        return encounterOfficeClassNotInCodings;
    }

    public List<Coding> getEncounterOfficeTypeInCodings() {  // for matching incoming ambulatory Encounters
        if (encounterOfficeTypeInCodings == null) {
            encounterOfficeTypeInCodings = buildCodings(env.getProperty("encounter.office.type.in.codings"));
        }
        return encounterOfficeTypeInCodings;
    }

    public List<Coding> getEncounterOfficeTypeNotInCodings() {  // for matching incoming ambulatory Encounters
        if (encounterOfficeTypeNotInCodings == null) {
            encounterOfficeTypeNotInCodings = buildCodings(env.getProperty("encounter.office.type.not-in.codings"));
        }
        return encounterOfficeTypeNotInCodings;
    }

    public List<Coding> getEncounterHomeClassInCodings() {  // for matching incoming home-health Encounters
        if (encounterHomeClassInCodings == null) {
            encounterHomeClassInCodings = buildCodings(env.getProperty("encounter.home.class.in.codings"));
        }
        return encounterHomeClassInCodings;
    }

    public List<Coding> getEncounterHomeClassNotInCodings() {  // for matching incoming home-health Encounters
        if (encounterHomeClassNotInCodings == null) {
            encounterHomeClassNotInCodings = buildCodings(env.getProperty("encounter.home.class.not-in.codings"));
        }
        return encounterHomeClassNotInCodings;
    }

    public List<Coding> getEncounterHomeTypeInCodings() {  // for matching incoming home-health Encounters
        if (encounterHomeTypeInCodings == null) {
            encounterHomeTypeInCodings = buildCodings(env.getProperty("encounter.home.type.in.codings"));
        }
        return encounterHomeTypeInCodings;
    }

    public List<Coding> getEncounterHomeTypeNotInCodings() {  // for matching incoming home-health Encounters
        if (encounterHomeTypeNotInCodings == null) {
            encounterHomeTypeNotInCodings = buildCodings(env.getProperty("encounter.home.type.not-in.codings"));
        }
        return encounterHomeTypeNotInCodings;
    }

    public String getEncounterLookbackPeriod() {
        return ENCOUNTER_LOOKBACK_PERIOD;
    }

    /**
     * @return a List of Codings that are used to positively identify a Resource as being in the Office context
     */
    public List<Coding> getBpOfficeCodings() {
        if (bpOfficeCodings == null) {
            bpOfficeCodings = buildCodings(env.getProperty("bp.office.codings"));
        }
        return bpOfficeCodings;
    }

    /**
     * @return a List of Codings that are used to positively identify a Resource as being in the Home context
     */
    public List<Coding> getBpHomeCodings() {
        if (bpHomeCodings == null) {
            bpHomeCodings = buildCodings(env.getProperty("bp.home.codings"));
        }
        return bpHomeCodings;
    }

    /**
     * @return a List of Codings that are used to positively identify a Resource as being a Panel (contains *both*
     *         systolic and diastolic components).
     */
    public List<Coding> getBpPanelCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpPanelCommonCoding());             // generic panel coding
        list.addAll(getBpPanelCustomCodings());         // any other panel codings specified by the user
        return list;
    }

    public Coding getBpPanelCommonCoding() {    // ANY source, may be home, office, inpatient, etc.
        return BP_PANEL_COMMON_CODING;
    }

    public List<Coding> getBpPanelCustomCodings() {
        if (bpPanelCustomCodings == null) {
            bpPanelCustomCodings = buildCodings(env.getProperty("bp.panel.custom-codings"));
        }
        return bpPanelCustomCodings;
    }

    public List<Coding> getBpSystolicCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpSystolicCommonCoding());          // generic systolic coding
        list.addAll(getBpSystolicCustomCodings());      // any other systolic codings specified by the user
        return list;
    }

    public Coding getBpSystolicCommonCoding() {     // ANY source, may be home, office, inpatient, etc.
        return BP_SYSTOLIC_COMMON_CODING;
    }

    public List<Coding> getBpSystolicCustomCodings() {
        if (bpSystolicCustomCodings == null) {
            bpSystolicCustomCodings = buildCodings(env.getProperty("bp.systolic.custom-codings"));
        }
        return bpSystolicCustomCodings;
    }

    public List<Coding> getBpDiastolicCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpDiastolicCommonCoding());         // generic diastolic coding
        list.addAll(getBpDiastolicCustomCodings());     // any other diastolic codings specified by the user
        return list;
    }

    public Coding getBpDiastolicCommonCoding() {    // ANY source, may be home, office, inpatient, etc.
        return BP_DIASTOLIC_COMMON_CODING;
    }


    public List<Coding> getBpDiastolicCustomCodings() {
        if (bpDiastolicCustomCodings == null) {
            bpDiastolicCustomCodings = buildCodings(env.getProperty("bp.diastolic.custom-codings"));
        }
        return bpDiastolicCustomCodings;
    }

    public String getBpValueCode() {
        return BP_VALUE_CODE;
    }

    public String getBpValueSystem() {
        return BP_VALUE_SYSTEM;
    }

    public String getBpValueUnit() {
        return BP_VALUE_UNIT;
    }

    public Integer getBpLimit() {
        return StringUtils.isEmpty(bpLimit) ?
                null :
                Integer.parseInt(bpLimit);
    }


    public String getBpLookbackPeriod() {
        return BP_LOOKBACK_PERIOD;
    }

    public List<Coding> getPulseCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getPulseCommonCoding());               // generic pulse coding
        list.addAll(getPulseCustomCodings());           // any other pulse codings specified by the user
        return list;
    }

    public Coding getPulseCommonCoding() {
        return PULSE_COMMON_CODING;
    }

    public List<Coding> getPulseCustomCodings() {
        if (pulseCustomCodings == null) {
            pulseCustomCodings = buildCodings(env.getProperty("pulse.custom-codings"));
        }
        return pulseCustomCodings;
    }

    public List<Coding> getServiceRequestOrderBPGoalCodings() {
        if (serviceRequestOrderBPGoalCodings == null) {
            serviceRequestOrderBPGoalCodings = buildCodings(env.getProperty("service-request-order.bp-goal.codings"));
        }
        return serviceRequestOrderBPGoalCodings;
    }

    public Pattern getServiceRequestOrderBPGoalNoteSystolicRegex() {
        if (serviceRequestOrderBPGoalNoteSystolicRegex == null) {
            serviceRequestOrderBPGoalNoteSystolicRegex = buildPattern(env.getProperty("service-request-order.bp-goal.note.systolic-regex"));
        }
        return serviceRequestOrderBPGoalNoteSystolicRegex;
    }

    public Pattern getServiceRequestOrderBPGoalNoteDiastolicRegex() {
        if (serviceRequestOrderBPGoalNoteDiastolicRegex == null) {
            serviceRequestOrderBPGoalNoteDiastolicRegex = buildPattern(env.getProperty("service-request-order.bp-goal.note.diastolic-regex"));
        }
        return serviceRequestOrderBPGoalNoteDiastolicRegex;
    }

    private Pattern buildPattern(String regex) {
        return StringUtils.isNotBlank(regex) ?
                Pattern.compile(regex) :
                null;
    }

    public String getPulseValueCode() {
        return PULSE_VALUE_CODE;
    }

    public String getPulseValueSystem() {
        return PULSE_VALUE_SYSTEM;
    }

    public String getPulseValueUnit() {
        return PULSE_VALUE_UNIT;
    }

    public String getPulseLookbackPeriod() {
        return PULSE_LOOKBACK_PERIOD;
    }

    public Coding getProtocolCoding() {
        return PROTOCOL_CODING;
    }

    public Coding getProtocolAnswerCoding() {
        return PROTOCOL_ANSWER_CODING;
    }

    public String getProtocolAnswerYes() {
        return protocolAnswerYes;
    }

    public String getProtocolAnswerNo() {
        return protocolAnswerNo;
    }

    public String getProtocolLookbackPeriod() {
        return PROTOCOL_LOOKBACK_PERIOD;
    }

    public Coding getBmiCoding() {
        return BMI_CODING;
    }

    public String getBmiLookbackPeriod() {
        return BMI_LOOKBACK_PERIOD;
    }

    public Coding getSmokingCoding() {
        return SMOKING_CODING;
    }

    public String getSmokingLookbackPeriod() {
        return SMOKING_LOOKBACK_PERIOD;
    }

    public Coding getDrinksCoding() {
        return DRINKS_CODING;
    }

    public String getDrinksLookbackPeriod() {
        return DRINKS_LOOKBACK_PERIOD;
    }

    public Coding getProcedureCounselingCoding() {
        return PROCEDURE_COUNSELING_CODING;
    }


///////////////////////////////////////////////////////////////////
// private methods
//

    private List<Coding> buildCodings(String s) {
        List<Coding> list = new ArrayList<>();
        if (StringUtils.isNotBlank(s)) {
            for (String s2 : s.split("\\s*(?<!\\\\),\\s*")) { // this will match "," so long it's not escaped ("\,")
                Coding c = buildCoding(s2);
                if (c != null) list.add(c);
            }
        }
        return list;
    }

    /**
     * build a FHIR Coding from a String
     * @param s a string of the form "system|code" or "system|code|display"
     *          Coding components may be blank; if they are, those components are ignored
     *          e.g., "system" -> Coding("system", null, null)
     *                "system|code" -> Coding("system", "code", null)
     *                "system|code|display" -> Coding("system", "code", "display")
     *                "|code" -> Coding(null, "code", null)
     *                "|code|display" -> Coding(null, "code", "display")
     *                "system||display" -> Coding("system", null, "display")
     *                "||display" -> Coding(null, null, "display")
     *
     * @return a populated FHIR Coding resource
     */
    private Coding buildCoding(String s) {
        if (StringUtils.isBlank(s)) return null;
        String[] parts = s.split("\\s*\\|\\s*");
        Coding c = new Coding();
        if (parts.length >= 1 && StringUtils.isNotBlank(parts[0])) c.setSystem(parts[0]);
        if (parts.length >= 2 && StringUtils.isNotBlank(parts[1])) c.setCode(parts[1]);
        if (parts.length >= 3 && StringUtils.isNotEmpty(parts[2])) c.setDisplay(parts[2]);
        return c;
    }
}