package edu.ohsu.cmp.coach.model;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.IncompatibleResourceException;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

public class MedicationModel implements Comparable<MedicationModel> {
    private String system;
    private String code;
    private String description;
    private String status;
    private Long effectiveTimestamp;

    // todo : populate these fields
    private String reason;
    private String dose;
    private String prescribingClinician;
    private String issues;
    private String priority;

    public MedicationModel(IBaseResource resource, Bundle bundle) throws DataException, IncompatibleResourceException {
        if (resource instanceof MedicationStatement) {
            createFromMedicationStatement((MedicationStatement) resource);

        } else if (resource instanceof MedicationRequest) {
            createFromMedicationRequest((MedicationRequest) resource, bundle);

        } else {
            throw new IncompatibleResourceException("cannot create MedicationModel from " + resource.getClass().getName());
        }
    }

    private void createFromMedicationStatement(MedicationStatement ms) throws DataException {
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

    private void createFromMedicationRequest(MedicationRequest mr, Bundle bundle) throws DataException {
        status = mr.getStatus().getDisplay();

        if (mr.hasMedicationCodeableConcept()) {
            CodeableConcept mcc = mr.getMedicationCodeableConcept();
//            description = mcc.getText();

            Coding c = mcc.getCodingFirstRep();
            system = c.getSystem();
            code = c.getCode();
            description = c.getDisplay();

        } else if (mr.hasMedicationReference()) {
            Medication m = FhirUtil.getResourceFromBundleByReference(bundle, Medication.class, mr.getMedicationReference().getReference());

            if (m != null && m.hasCode()) {
                Coding c = m.getCode().getCodingFirstRep();
                system = c.getSystem();
                code = c.getCode();
                description = c.getDisplay();

            } else {
                throw new DataException("medication or medication code not found: " + mr.getMedicationReference().getReference());
            }
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

        prescribingClinician = ""; // todo : set this.  not quite clear how to do that cleanly with MedicationRequest

        issues = mr.hasDetectedIssue() ? mr.getDetectedIssueFirstRep().getDisplay() : "";

        priority = mr.hasPriority() ? mr.getPriority().getDisplay() : "";
    }

    @Override
    public int compareTo(MedicationModel o) {
        return 0;   // todo: implement this
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
