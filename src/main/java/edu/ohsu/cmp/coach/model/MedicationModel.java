package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;

public class MedicationModel {

    private MedicationStatement sourceMedicationStatement;
    private MedicationRequest sourceMedicationRequest;
    private Medication sourceMedicationRequestMedication;

    private String system;
    private String code;
    private String description;
    private String status;
    private Long effectiveTimestamp;
    private String reason;
    private String dose;
    private String prescribingClinician;
    private String issues;
    private String priority;

    public MedicationModel(MedicationStatement ms) throws DataException {
        sourceMedicationStatement = ms;

        status = ms.getStatus().getDisplay();

        CodeableConcept mcc = ms.getMedicationCodeableConcept();
        description = mcc.getText();

        if (mcc.getCoding().size() > 0) {       // only grab the first coding
            Coding c = mcc.getCoding().get(0);
            system = c.getSystem();
            code = c.getCode();
        }

        if (ms.getEffectiveDateTimeType() != null) {
            effectiveTimestamp = ms.getEffectiveDateTimeType().getValue().getTime();

        } else if (ms.getEffectivePeriod() != null) {
            effectiveTimestamp = ms.getEffectivePeriod().getEnd().getTime();

        } else {
            throw new DataException("missing effective date or period");
        }

        if (ms.hasReasonCode()) {
            reason = ms.getReasonCodeFirstRep().getText();

        } else if (ms.hasReasonReference()) {
            // todo : grab the referenced resource and set reason = that resource's most appropriate description
            reason = ms.getReasonReferenceFirstRep().getReference();

        } else {
            reason = "";
        }

        dose = ms.hasDosage() ? ms.getDosageFirstRep().getText() : "";

        prescribingClinician = ""; // todo : set this.  not quite clear how to do that cleanly with MedicationStatement

        issues = ""; // todo : set this.  not sure what this should be.  only here because it's specified in the MCC app,
        //        and Dave wants this app to mirror the MCC system with respect to display of medications.
        //        there isn't anything in the MedicationStatement resource that appears to fit

        priority = "";  // todo : not sure where this should come from either
    }

    public MedicationModel(MedicationRequest mr, Bundle bundle) throws DataException {
        sourceMedicationRequest = mr;

        status = mr.getStatus().getDisplay();

        CodeableConcept cc;
        if (mr.hasMedicationCodeableConcept()) {
            cc = mr.getMedicationCodeableConcept();

        } else if (mr.hasMedicationReference()) {
            String reference = mr.getMedicationReference().getReference();
            Medication m = FhirUtil.getResourceFromBundleByReference(bundle, Medication.class, reference);
            if (m == null) {
                throw new DataException("Medication not found in bundle: " + reference);

            } else if (m.hasCode()) {
                sourceMedicationRequestMedication = m;
                cc = m.getCode();

            } else {
                throw new DataException("no code block found for Medication: " + reference);
            }

        } else {
            throw new DataException("MedicationRequest appears to be missing both MedicationCodeableConcept and MedicationReference: " + mr.getId());
        }

        Coding c = cc.getCodingFirstRep();
        system = c.getSystem();
        code = c.getCode();

        if (c.hasDisplay()) {
            description = c.getDisplay();

        } else if (cc.hasText()) {
            description = cc.getText();

        } else {
            throw new DataException("no description available for MedicationRequest " + mr.getId());
        }

        if (mr.getAuthoredOn() != null) {
            effectiveTimestamp = mr.getAuthoredOn().getTime();

        } else {
            throw new DataException("missing authored on");
        }

        if (mr.hasReasonCode()) {
            reason = mr.getReasonCodeFirstRep().getText();

        } else if (mr.hasReasonReference()) {
            // todo : grab the referenced resource and set reason = that resource's most appropriate description
            reason = mr.getReasonReferenceFirstRep().getReference();

        } else {
            reason = "";
        }

        dose = mr.hasDosageInstruction() ? mr.getDosageInstructionFirstRep().getText() : "";

        // "recorder" is how/where it appears to be stored in POC, using that for now.
        prescribingClinician = mr.hasRecorder() ?
                mr.getRecorder().getDisplay() :
                "";

        issues = mr.hasDetectedIssue() ? mr.getDetectedIssueFirstRep().getDisplay() : "";

        priority = mr.hasPriority() ? mr.getPriority().getDisplay() : "";
    }

    public boolean matches(String system, String code) {
        if (this.system.equals(system) && this.code.equals(code)) {
            return true;

        } else if (sourceMedicationStatement != null) {
            for (Coding c : sourceMedicationStatement.getMedicationCodeableConcept().getCoding()) {
                if (c.getSystem().equals(system) && c.getCode().equals(code)) {
                    return true;
                }
            }

        } else if (sourceMedicationRequest != null) {
            if (sourceMedicationRequest.hasMedicationCodeableConcept()) {
                for (Coding c : sourceMedicationRequest.getMedicationCodeableConcept().getCoding()) {
                    if (c.getSystem().equals(system) && c.getCode().equals(code)) {
                        return true;
                    }
                }

            } else if (sourceMedicationRequest.hasMedicationReference() && sourceMedicationRequestMedication != null) {
                for (Coding c : sourceMedicationRequestMedication.getCode().getCoding()) {
                    if (c.getSystem().equals(system) && c.getCode().equals(code)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @JsonIgnore
    public MedicationStatement getSourceMedicationStatement() {
        return sourceMedicationStatement;
    }

    @JsonIgnore
    public MedicationRequest getSourceMedicationRequest() {
        return sourceMedicationRequest;
    }

    @JsonIgnore
    public Medication getSourceMedicationRequestMedication() {
        return sourceMedicationRequestMedication;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getEffectiveTimestamp() {
        return effectiveTimestamp;
    }

    public void setEffectiveTimestamp(Long effectiveTimestamp) {
        this.effectiveTimestamp = effectiveTimestamp;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getPrescribingClinician() {
        return prescribingClinician;
    }

    public void setPrescribingClinician(String prescribingClinician) {
        this.prescribingClinician = prescribingClinician;
    }

    public String getIssues() {
        return issues;
    }

    public void setIssues(String issues) {
        this.issues = issues;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
