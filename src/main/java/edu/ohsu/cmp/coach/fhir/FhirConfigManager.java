package edu.ohsu.cmp.coach.fhir;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("${fhirconfig.file}")
public class FhirConfigManager {

    @Value("${bp.system}")          private String bpSystem;
    @Value("${bp.code}")            private String bpCode;
    @Value("${bp.systolic.code}")   private String bpSystolicCode;
    @Value("${bp.diastolic.code}")  private String bpDiastolicCode;
    @Value("${bp.home.system:}")    private String bpHomeSystem;    // optional
    @Value("${bp.home.code:}")      private String bpHomeCode;      // optional
    @Value("${bp.home.display:}")   private String bpHomeDisplay;   // optional
    @Value("${bp.value.code}")      private String bpValueCode;
    @Value("${bp.value.system}")    private String bpValueSystem;
    @Value("${bp.value.unit}")      private String bpValueUnit;

    @Value("${medication.valueset.oid}")    private String medicationValueSetOid;

    public String getBpSystem() {
        return bpSystem;
    }

    public String getBpCode() {
        return bpCode;
    }

    public String getBpSystolicCode() {
        return bpSystolicCode;
    }

    public String getBpDiastolicCode() {
        return bpDiastolicCode;
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

    public String getMedicationValueSetOid() {
        return medicationValueSetOid;
    }
}
