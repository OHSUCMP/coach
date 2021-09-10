package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import edu.ohsu.cmp.htnu18app.exception.IncompatibleResourceException;
import edu.ohsu.cmp.htnu18app.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

public class MedicationModel implements Comparable<MedicationModel> {
    private String system;
    private String code;
    private String description;
    private String status;
    private Long effectiveTimestamp;

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
}
