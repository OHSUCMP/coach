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

@Component
@PropertySource("${fhirconfig.file}")
public class FhirConfigManager {

    private static final String ENCOUNTER_LOOKBACK_PERIOD = "2y";
    private static final Coding BP_CODING = new Coding("http://loinc.org", "55284-4", "Blood pressure systolic and diastolic");
    private static final Coding BP_SYSTOLIC_CODING = new Coding("http://loinc.org", "8480-6", "Systolic blood pressure");
    private static final Coding BP_DIASTOLIC_CODING = new Coding("http://loinc.org", "8462-4", "Diastolic blood pressure");
    private static final String BP_VALUE_CODE = "mm[Hg]";
    private static final String BP_VALUE_SYSTEM = "http://unitsofmeasure.org";
    private static final String BP_VALUE_UNIT = "mmHg";
    private static final String BP_LOOKBACK_PERIOD = "2y";
    private static final Coding PULSE_CODING = new Coding("http://loinc.org", "8867-4", "Heart rate");
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

    @Value("${encounter.class.system}")         private String encounterClassSystem;
    @Value("${encounter.class.amb.code}")       private String encounterClassAMBCode;
    @Value("${encounter.class.amb.display}")    private String encounterClassAMBDisplay;
    @Value("${encounter.class.hh.code}")        private String encounterClassHHCode;
    @Value("${encounter.class.hh.display}")     private String encounterClassHHDisplay;

    @Value("${encounter.amb.class.in}")         private String encounterAmbClassIn;
    @Value("${encounter.amb.class.not-in}")     private String encounterAmbClassNotIn;
    @Value("${encounter.amb.type.in}")          private String encounterAmbTypeIn;
    @Value("${encounter.amb.type.not-in}")      private String encounterAmbTypeNotIn;
    @Value("${encounter.hh.class.in}")         private String encounterHHClassIn;
    @Value("${encounter.hh.class.not-in}")     private String encounterHHClassNotIn;
    @Value("${encounter.hh.type.in}")          private String encounterHHTypeIn;
    @Value("${encounter.hh.type.not-in}")      private String encounterHHTypeNotIn;
//    @Value("${encounter.lookbackPeriod}")      private String encounterLookbackPeriod;

//    private Coding bpCoding = null;
//    private Coding bpSystolicCoding = null;
//    private Coding bpDiastolicCoding = null;
    private List<Coding> bpOfficeCodings = null;
    private List<Coding> bpHomeCodings = null;
    private Coding bpEpicSystolicCoding = null;
    private Coding bpEpicDiastolicCoding = null;
//    @Value("${bp.value.code}")      private String bpValueCode;
//    @Value("${bp.value.system}")    private String bpValueSystem;
//    @Value("${bp.value.unit}")      private String bpValueUnit;
    @Value("${bp.limit}")           private String bpLimit;
//    @Value("${bp.lookbackPeriod}")  private String bpLookbackPeriod;
//    private Coding pulseCoding = null;
    private Coding pulseEpicCoding = null;
//    @Value("${pulse.value.code}")   private String pulseValueCode;
//    @Value("${pulse.value.system}") private String pulseValueSystem;
//    @Value("${pulse.value.unit}")   private String pulseValueUnit;
//    @Value("${pulse.lookbackPeriod}")       private String pulseLookbackPeriod;
//    private Coding protocolCoding = null;
//    private Coding protocolAnswerCoding = null;
    @Value("${protocol.answer.yes}")        private String protocolAnswerYes;
    @Value("${protocol.answer.no}")         private String protocolAnswerNo;
//    @Value("${protocol.lookbackPeriod}")    private String protocolLookbackPeriod;
//    private Coding bmiCoding = null;
//    @Value("${bmi.lookbackPeriod}")         private String bmiLookbackPeriod;
//    private Coding smokingCoding = null;
//    @Value("${smoking.lookbackPeriod}")     private String smokingLookbackPeriod;
//    private Coding drinksCoding = null;
//    @Value("${drinks.lookbackPeriod}")      private String drinksLookbackPeriod;
//    private Coding procedureCounselingCoding = null;


    public String getEncounterClassSystem() {
        return encounterClassSystem;
    }

    public String getEncounterClassAMBCode() {
        return encounterClassAMBCode;
    }

    public String getEncounterClassAMBDisplay() {
        return encounterClassAMBDisplay;
    }

    public String getEncounterClassHHCode() {
        return encounterClassHHCode;
    }

    public String getEncounterClassHHDisplay() {
        return encounterClassHHDisplay;
    }

    public String getEncounterAmbClassIn() {
        return encounterAmbClassIn;
    }

    public String getEncounterAmbClassNotIn() {
        return encounterAmbClassNotIn;
    }

    public String getEncounterAmbTypeIn() {
        return encounterAmbTypeIn;
    }

    public String getEncounterAmbTypeNotIn() {
        return encounterAmbTypeNotIn;
    }

    public String getEncounterHHClassIn() {
        return encounterHHClassIn;
    }

    public String getEncounterHHClassNotIn() {
        return encounterHHClassNotIn;
    }

    public String getEncounterHHTypeIn() {
        return encounterHHTypeIn;
    }

    public String getEncounterHHTypeNotIn() {
        return encounterHHTypeNotIn;
    }

    public String getEncounterLookbackPeriod() {
        return ENCOUNTER_LOOKBACK_PERIOD;
    }

    public Coding getBpCoding() {       // ANY source, may be home, office, inpatient, etc.
        return BP_CODING;
    }

    public Coding getBpSystolicCoding() {   // ANY source, may be home, office, inpatient, etc.
        return BP_SYSTOLIC_CODING;
    }

    public Coding getBpDiastolicCoding() {  // ANY source, may be home, office, inpatient, etc.
        return BP_DIASTOLIC_CODING;
    }

    public List<Coding> getBpOfficeCodings() {  // specifically office
        if (bpOfficeCodings == null) {
            bpOfficeCodings = buildCodings(env.getProperty("bp.office.codings"));
        }
        return bpOfficeCodings;
    }

    public boolean hasBpHomeCodings() {
        return StringUtils.isNotEmpty(env.getProperty("bp.home.codings"));
    }

    public List<Coding> getBpHomeCodings() {    // specifically home
        if (bpHomeCodings == null) {
            bpHomeCodings = buildCodings(env.getProperty("bp.home.codings"));
        }
        return bpHomeCodings;
    }

    public Coding getBpEpicSystolicCoding() {   // specifically home
        if (bpEpicSystolicCoding == null) {
            bpEpicSystolicCoding = buildCoding(env.getProperty("bp.epic.systolic.coding"));
        }
        return bpEpicSystolicCoding;
    }

    public Coding getBpEpicDiastolicCoding() {  // specifically home
        if (bpEpicDiastolicCoding == null) {
            bpEpicDiastolicCoding = buildCoding(env.getProperty("bp.epic.diastolic.coding"));
        }
        return bpEpicDiastolicCoding;
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

    public List<Coding> getAllBpCodings() {
        List<Coding> list = new ArrayList<>();
        list.addAll(getBpPanelCodings());
        list.addAll(getBpHomeCodings());
        list.addAll(getSystolicCodings());
        list.addAll(getDiastolicCodings());
        return list;
    }

    public List<Coding> getBpPanelCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpCoding());            // generic panel
        list.addAll(getBpOfficeCodings());  // office panel
//        list.addAll(getBpHomeCodings());    // values are also appended to individual systolic and diastolic readings
        return list;
    }

    public List<Coding> getSystolicCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpSystolicCoding());        // generic
        list.add(getBpEpicSystolicCoding());    // epic home reading flowsheet
        return list;
    }

    public List<Coding> getDiastolicCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpDiastolicCoding());       // generic
        list.add(getBpEpicDiastolicCoding());   // epic home reading flowsheet
        return list;
    }

    public String getBpLookbackPeriod() {
        return BP_LOOKBACK_PERIOD;
    }

    public Coding getPulseCoding() {
        return PULSE_CODING;
    }

    public Coding getPulseEpicCoding() {
        if (pulseEpicCoding == null) {
            pulseEpicCoding = buildCoding(env.getProperty("pulse.epic.coding"));
        }
        return pulseEpicCoding;
    }

    public List<Coding> getPulseCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getPulseCoding());
        list.add(getPulseEpicCoding());
        return list;
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
        if (s != null) {
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
     * @return a populated FHIR Coding resource
     */
    private Coding buildCoding(String s) {
        if (s == null) return null;
        String[] parts = s.split("\\|");
        Coding c = new Coding().setSystem(parts[0]).setCode(parts[1]);
        if (parts.length > 2) c.setDisplay(parts[2]);
        return c;
    }
}
