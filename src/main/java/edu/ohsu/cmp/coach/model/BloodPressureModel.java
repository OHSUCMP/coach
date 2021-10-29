package edu.ohsu.cmp.coach.model;

import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;

public class BloodPressureModel {
//    public static final String SYSTEM = "http://loinc.org";
//    public static final String CODE = "55284-4";
//    public static final String SYSTOLIC_CODE = "8480-6";
//    public static final String DIASTOLIC_CODE = "8462-4";
//    public static final String VALUE_SYSTEM = "http://unitsofmeasure.org";
//    public static final String VALUE_CODE = "mm[Hg]";
//    public static final String VALUE_UNIT = "mmHg";

    private Source source;
    private QuantityModel systolic;
    private QuantityModel diastolic;
    private Long timestamp;

    public enum Source {
        OFFICE,
        HOME
    }

    private enum ValueType {
        SYSTOLIC,
        DIASTOLIC,
        OTHER,
        UNKNOWN
    }

    public BloodPressureModel(HomeBloodPressureReading reading) {
        source = Source.HOME;
        systolic = new QuantityModel(reading.getSystolic(), "mmHg");
        diastolic = new QuantityModel(reading.getDiastolic(), "mmHg");
        timestamp = reading.getReadingDate().getTime();
    }

    public BloodPressureModel(Observation o, FhirConfigManager fcm) throws DataException {
        source = Source.OFFICE;
        for (Observation.ObservationComponentComponent occ : o.getComponent()) {
            ValueType valueType = ValueType.UNKNOWN;

            CodeableConcept cc = occ.getCode();
            for (Coding c : cc.getCoding()) {
                if (c.getSystem().equals(fcm.getBpSystem()) && c.getCode().equals(fcm.getBpSystolicCode())) {
                    valueType = ValueType.SYSTOLIC;
                    break;

                } else if (c.getSystem().equals(fcm.getBpSystem()) && c.getCode().equals(fcm.getBpDiastolicCode())) {
                    valueType = ValueType.DIASTOLIC;
                    break;
                }
            }

            if (valueType != ValueType.UNKNOWN) {
                Quantity q = occ.getValueQuantity();
                switch (valueType) {
                    case SYSTOLIC: systolic = new QuantityModel(q); break;
                    case DIASTOLIC: diastolic = new QuantityModel(q); break;
                }
            }
        }

        if (o.getEffectiveDateTimeType() != null) {
            this.timestamp = o.getEffectiveDateTimeType().getValue().getTime();

        } else if (o.getEffectiveInstantType() != null) {
            this.timestamp = o.getEffectiveInstantType().getValue().getTime();

        } else if (o.getEffectivePeriod() != null) {
            this.timestamp = o.getEffectivePeriod().getEnd().getTime();

        } else {
            throw new DataException("missing timestamp");
        }
    }

    public Source getSource() {
        return source;
    }

    public QuantityModel getSystolic() {
        return systolic;
    }

    public QuantityModel getDiastolic() {
        return diastolic;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}
