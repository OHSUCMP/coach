package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.exception.DataException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MedicationStatement;

public class MedicationModel implements Comparable<MedicationModel> {
    public static final String VALUE_SET_OID = "2.16.840.1.113762.1.4.1178.10";

    private String system;
    private String code;
    private String description;
    private String status;
    private Long effectiveTimestamp;

    public MedicationModel(MedicationStatement ms) throws DataException {
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
