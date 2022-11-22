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
    @Value("${encounter.lookbackPeriod}")      private String encounterLookbackPeriod;

    private Coding bpCoding = null;
    private Coding bpSystolicCoding = null;
    private Coding bpDiastolicCoding = null;
    private List<Coding> bpOfficeCodings = null;
    private List<Coding> bpHomeCodings = null;
    private Coding bpEpicSystolicCoding = null;
    private Coding bpEpicDiastolicCoding = null;
    @Value("${bp.value.code}")      private String bpValueCode;
    @Value("${bp.value.system}")    private String bpValueSystem;
    @Value("${bp.value.unit}")      private String bpValueUnit;
    @Value("${bp.limit}")           private String bpLimit;
    @Value("${bp.lookbackPeriod}")  private String bpLookbackPeriod;
    private Coding pulseCoding = null;
    private Coding pulseEpicCoding = null;
    @Value("${pulse.value.code}")   private String pulseValueCode;
    @Value("${pulse.value.system}") private String pulseValueSystem;
    @Value("${pulse.value.unit}")   private String pulseValueUnit;
    @Value("${pulse.lookbackPeriod}")       private String pulseLookbackPeriod;
    private Coding protocolCoding = null;
    private Coding protocolAnswerCoding = null;
    @Value("${protocol.answer.yes}")        private String protocolAnswerYes;
    @Value("${protocol.answer.no}")         private String protocolAnswerNo;
    @Value("${protocol.lookbackPeriod}")    private String protocolLookbackPeriod;
    private Coding bmiCoding = null;
    @Value("${bmi.lookbackPeriod}")         private String bmiLookbackPeriod;
    private Coding smokingCoding = null;
    @Value("${smoking.lookbackPeriod}")     private String smokingLookbackPeriod;
    private Coding drinksCoding = null;
    @Value("${drinks.lookbackPeriod}")      private String drinksLookbackPeriod;
    private Coding procedureCounselingCoding = null;


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
        return encounterLookbackPeriod;
    }

    public Coding getBpCoding() {
        if (bpCoding == null) {
            bpCoding = buildCoding(env.getProperty("bp.coding"));
        }
        return bpCoding;
    }

    public Coding getBpSystolicCoding() {
        if (bpSystolicCoding == null) {
            bpSystolicCoding = buildCoding(env.getProperty("bp.systolic.coding"));
        }
        return bpSystolicCoding;
    }

    public Coding getBpDiastolicCoding() {
        if (bpDiastolicCoding == null) {
            bpDiastolicCoding = buildCoding(env.getProperty("bp.diastolic.coding"));
        }
        return bpDiastolicCoding;
    }

    public List<Coding> getBpOfficeCodings() {
        if (bpOfficeCodings == null) {
            bpOfficeCodings = buildCodings(env.getProperty("bp.office.codings"));
        }
        return bpOfficeCodings;
    }

    public boolean hasBpHomeCodings() {
        return StringUtils.isNotEmpty(env.getProperty("bp.home.codings"));
    }

    public List<Coding> getBpHomeCodings() {
        if (bpHomeCodings == null) {
            bpHomeCodings = buildCodings(env.getProperty("bp.home.codings"));
        }
        return bpHomeCodings;
    }

    public Coding getBpEpicSystolicCoding() {
        if (bpEpicSystolicCoding == null) {
            bpEpicSystolicCoding = buildCoding(env.getProperty("bp.epic.systolic.coding"));
        }
        return bpEpicSystolicCoding;
    }

    public Coding getBpEpicDiastolicCoding() {
        if (bpEpicDiastolicCoding == null) {
            bpEpicDiastolicCoding = buildCoding(env.getProperty("bp.epic.diastolic.coding"));
        }
        return bpEpicDiastolicCoding;
    }

    public String getBpValueCode() {
        return bpValueCode;
    }

    public String getBpValueSystem() {
        return bpValueSystem;
    }

    public String getBpValueUnit() {
        return bpValueUnit;
    }

    public Integer getBpLimit() {
        return StringUtils.isEmpty(bpLimit) ?
                null :
                Integer.parseInt(bpLimit);
    }

    public List<Coding> getAllBpCodings() {
        List<Coding> list = new ArrayList<>();
        list.addAll(getBpPanelCodings());
        list.addAll(getSystolicCodings());
        list.addAll(getDiastolicCodings());
        return list;
    }

    public List<Coding> getBpPanelCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpCoding());
        list.addAll(getBpOfficeCodings());
//        list.addAll(getBpHomeCodings());      // home codings are also appended to individual systolic and diastolic readings
        return list;
    }

    public List<Coding> getSystolicCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpSystolicCoding());
        list.add(getBpEpicSystolicCoding());
        return list;
    }

    public List<Coding> getDiastolicCodings() {
        List<Coding> list = new ArrayList<>();
        list.add(getBpDiastolicCoding());
        list.add(getBpEpicDiastolicCoding());
        return list;
    }

    public String getBpLookbackPeriod() {
        return bpLookbackPeriod;
    }

    public Coding getPulseCoding() {
        if (pulseCoding == null) {
            pulseCoding = buildCoding(env.getProperty("pulse.coding"));
        }
        return pulseCoding;
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
        return pulseValueCode;
    }

    public String getPulseValueSystem() {
        return pulseValueSystem;
    }

    public String getPulseValueUnit() {
        return pulseValueUnit;
    }

    public String getPulseLookbackPeriod() {
        return pulseLookbackPeriod;
    }

    public Coding getProtocolCoding() {
        if (protocolCoding == null) {
            protocolCoding = buildCoding(env.getProperty("protocol.coding"));
        }
        return protocolCoding;
    }

    public Coding getProtocolAnswerCoding() {
        if (protocolAnswerCoding == null) {
            protocolAnswerCoding = buildCoding(env.getProperty("protocol.answer.coding"));
        }
        return protocolAnswerCoding;
    }

    public String getProtocolAnswerYes() {
        return protocolAnswerYes;
    }

    public String getProtocolAnswerNo() {
        return protocolAnswerNo;
    }

    public String getProtocolLookbackPeriod() {
        return protocolLookbackPeriod;
    }

    public Coding getBmiCoding() {
        if (bmiCoding == null) {
            bmiCoding = buildCoding(env.getProperty("bmi.coding"));
        }
        return bmiCoding;
    }

    public String getBmiLookbackPeriod() {
        return bmiLookbackPeriod;
    }

    public Coding getSmokingCoding() {
        if (smokingCoding == null) {
            smokingCoding = buildCoding(env.getProperty("smoking.coding"));
        }
        return smokingCoding;
    }

    public String getSmokingLookbackPeriod() {
        return smokingLookbackPeriod;
    }

    public Coding getDrinksCoding() {
        if (drinksCoding == null) {
            drinksCoding = buildCoding(env.getProperty("drinks.coding"));
        }
        return drinksCoding;
    }

    public String getDrinksLookbackPeriod() {
        return drinksLookbackPeriod;
    }

    public Coding getProcedureCounselingCoding() {
        if (procedureCounselingCoding == null) {
            procedureCounselingCoding = buildCoding(env.getProperty("procedure.counseling.coding"));
        }
        return procedureCounselingCoding;
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
