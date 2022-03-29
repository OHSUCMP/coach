package edu.ohsu.cmp.coach.fhir;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("${fhirconfig.file}")
public class FhirConfigManager {

    @Value("${encounter.class.system}")         private String encounterClassSystem;
    @Value("${encounter.class.amb.code}")       private String encounterClassAMBCode;
    @Value("${encounter.class.amb.display}")    private String encounterClassAMBDisplay;
    @Value("${encounter.class.hh.code}")        private String encounterClassHHCode;
    @Value("${encounter.class.hh.display}")     private String encounterClassHHDisplay;

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

    @Value("${pulse.code}")         private String pulseCode;
    @Value("${pulse.system}")       private String pulseSystem;
    @Value("${pulse.display}")      private String pulseDisplay;
    @Value("${pulse.value.code}")   private String pulseValueCode;
    @Value("${pulse.value.system}") private String pulseValueSystem;
    @Value("${pulse.value.unit}")   private String pulseValueUnit;

    @Value("${protocol.code}")              private String protocolCode;
    @Value("${protocol.system}")            private String protocolSystem;
    @Value("${protocol.display}")           private String protocolDisplay;
    @Value("${protocol.answer.code}")       private String protocolAnswerCode;
    @Value("${protocol.answer.system}")     private String protocolAnswerSystem;
    @Value("${protocol.answer.display}")    private String protocolAnswerDisplay;
    @Value("${protocol.answer.yes}")        private String protocolAnswerYes;
    @Value("${protocol.answer.no}")         private String protocolAnswerNo;

    @Value("${medication.valueset.oid}")    private String medicationValueSetOid;

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

    public String getMedicationValueSetOid() {
        return medicationValueSetOid;
    }
}
