package edu.ohsu.cmp.coach.fhir;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@PropertySource("${fhirconfig.file}")
public class FhirConfigManager {

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

    @Value("${bp.system}")          private String bpSystem;
    @Value("${bp.code}")            private String bpCode;
    @Value("${bp.display}")         private String bpDisplay;
    @Value("${bp.systolic.code}")       private String bpSystolicCode;
    @Value("${bp.systolic.display}")    private String bpSystolicDisplay;
    @Value("${bp.diastolic.code}")      private String bpDiastolicCode;
    @Value("${bp.diastolic.display}")   private String bpDiastolicDisplay;
    @Value("${bp.home.system:}")    private String bpHomeSystem;    // optional - that's what the ":" is in the token
    @Value("${bp.home.code:}")      private String bpHomeCode;      // optional - that's what the ":" is in the token
    @Value("${bp.home.display:}")   private String bpHomeDisplay;   // optional - that's what the ":" is in the token
    @Value("${bp.value.code}")      private String bpValueCode;
    @Value("${bp.value.system}")    private String bpValueSystem;
    @Value("${bp.value.unit}")      private String bpValueUnit;
    @Value("${bp.limit}")           private String bpLimit;
    @Value("${bp.lookbackPeriod}")  private String bpLookbackPeriod;

    @Value("${pulse.code}")         private String pulseCode;
    @Value("${pulse.system}")       private String pulseSystem;
    @Value("${pulse.display}")      private String pulseDisplay;
    @Value("${pulse.value.code}")   private String pulseValueCode;
    @Value("${pulse.value.system}") private String pulseValueSystem;
    @Value("${pulse.value.unit}")   private String pulseValueUnit;
    @Value("${pulse.lookbackPeriod}")       private String pulseLookbackPeriod;

    @Value("${protocol.code}")              private String protocolCode;
    @Value("${protocol.system}")            private String protocolSystem;
    @Value("${protocol.display}")           private String protocolDisplay;
    @Value("${protocol.answer.code}")       private String protocolAnswerCode;
    @Value("${protocol.answer.system}")     private String protocolAnswerSystem;
    @Value("${protocol.answer.display}")    private String protocolAnswerDisplay;
    @Value("${protocol.answer.yes}")        private String protocolAnswerYes;
    @Value("${protocol.answer.no}")         private String protocolAnswerNo;
    @Value("${protocol.lookbackPeriod}")    private String protocolLookbackPeriod;

    @Value("${bmi.code}")                   private String bmiCode;
    @Value("${bmi.system}")                 private String bmiSystem;
    @Value("${bmi.lookbackPeriod}")         private String bmiLookbackPeriod;

    @Value("${smoking.code}")               private String smokingCode;
    @Value("${smoking.system}")             private String smokingSystem;
    @Value("${smoking.lookbackPeriod}")     private String smokingLookbackPeriod;

    @Value("${drinks.code}")                private String drinksCode;
    @Value("${drinks.system}")              private String drinksSystem;
    @Value("${drinks.lookbackPeriod}")      private String drinksLookbackPeriod;

    @Value("${procedure.counseling.system}")    private String procedureCounselingSystem;
    @Value("${procedure.counseling.code}")      private String procedureCounselingCode;


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

    public String getBpSystem() {
        return bpSystem;
    }

    public String getBpCode() {
        return bpCode;
    }

    public String getBpDisplay() {
        return bpDisplay;
    }

    public String getBpSystolicCode() {
        return bpSystolicCode;
    }

    public String getBpSystolicDisplay() {
        return bpSystolicDisplay;
    }

    public String getBpDiastolicCode() {
        return bpDiastolicCode;
    }

    public String getBpDiastolicDisplay() {
        return bpDiastolicDisplay;
    }

    public String getBpHomeSystem() {
        return bpHomeSystem;
    }

    public String getBpHomeCode() {
        return bpHomeCode;
    }

    public String getBpHomeDisplay() {
        return bpHomeDisplay;
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

    public String getBpLookbackPeriod() {
        return bpLookbackPeriod;
    }

    public String getPulseCode() {
        return pulseCode;
    }

    public String getPulseSystem() {
        return pulseSystem;
    }

    public String getPulseDisplay() {
        return pulseDisplay;
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

    public String getProtocolCode() {
        return protocolCode;
    }

    public String getProtocolSystem() {
        return protocolSystem;
    }

    public String getProtocolDisplay() {
        return protocolDisplay;
    }

    public String getProtocolAnswerCode() {
        return protocolAnswerCode;
    }

    public String getProtocolAnswerSystem() {
        return protocolAnswerSystem;
    }

    public String getProtocolAnswerDisplay() {
        return protocolAnswerDisplay;
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

    public String getBmiCode() {
        return bmiCode;
    }

    public String getBmiSystem() {
        return bmiSystem;
    }

    public String getBmiLookbackPeriod() {
        return bmiLookbackPeriod;
    }

    public String getSmokingCode() {
        return smokingCode;
    }

    public String getSmokingSystem() {
        return smokingSystem;
    }

    public String getSmokingLookbackPeriod() {
        return smokingLookbackPeriod;
    }

    public String getDrinksCode() {
        return drinksCode;
    }

    public String getDrinksSystem() {
        return drinksSystem;
    }

    public String getDrinksLookbackPeriod() {
        return drinksLookbackPeriod;
    }

    public String getProcedureCounselingSystem() {
        return procedureCounselingSystem;
    }

    public String getProcedureCounselingCode() {
        return procedureCounselingCode;
    }
}
