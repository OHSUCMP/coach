package edu.ohsu.cmp.htnu18app.model;

import edu.ohsu.cmp.htnu18app.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.exception.DataException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;

import java.util.Objects;

public class BloodPressureModel implements Comparable<BloodPressureModel> {
    public static final String SYSTEM = "http://loinc.org";
    public static final String CODE = "55284-4";
    public static final String SYSTOLIC_CODE = "8480-6";
    public static final String DIASTOLIC_CODE = "8462-4";
    public static final String VALUE_SYSTEM = "http://unitsofmeasure.org";
    public static final String VALUE_CODE = "mm[Hg]";
    public static final String VALUE_UNIT = "mmHg";

    private QuantityModel systolic;
    private QuantityModel diastolic;
    private Long timestamp;

    @Override
    public int compareTo(BloodPressureModel o) {
        return timestamp.compareTo(o.timestamp) * -1; // reverse chronological order, most recent first
    }

// adapted from https://stackoverflow.com/questions/5038204/apache-commons-equals-hashcode-builder

    @Override
    public int hashCode() {
        return Objects.hash(systolic, diastolic, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BloodPressureModel) {
            final BloodPressureModel other = (BloodPressureModel) obj;
            return Objects.equals(systolic, other.systolic)
                    && Objects.equals(diastolic, other.diastolic)
                    && Objects.equals(timestamp, other.timestamp);
        } else {
            return false;
        }
    }

    private enum ValueType {
        SYSTOLIC,
        DIASTOLIC,
        OTHER,
        UNKNOWN
    }

    public BloodPressureModel(HomeBloodPressureReading reading) {
        systolic = new QuantityModel(reading.getSystolic(), "mmHg");
        diastolic = new QuantityModel(reading.getDiastolic(), "mmHg");
        timestamp = reading.getReadingDate().getTime();
    }

    public BloodPressureModel(Observation o) throws DataException {
        for (Observation.ObservationComponentComponent occ : o.getComponent()) {
            ValueType valueType = ValueType.UNKNOWN;

            CodeableConcept cc = occ.getCode();
            for (Coding c : cc.getCoding()) {
                if (c.getSystem().equals(SYSTEM) && c.getCode().equals(SYSTOLIC_CODE)) {
                    valueType = ValueType.SYSTOLIC;
                    break;

                } else if (c.getSystem().equals(SYSTEM) && c.getCode().equals(DIASTOLIC_CODE)) {
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
