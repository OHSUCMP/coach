package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.util.ObservationUtil;
import org.hl7.fhir.r4.model.*;

import java.util.Date;

public class BloodPressureModel extends AbstractVitalsModel {
    private Observation sourceBPObservation;

    private QuantityModel systolic = null;
    private QuantityModel diastolic = null;

    private enum ValueType {
        SYSTOLIC,
        DIASTOLIC,
        OTHER,
        UNKNOWN
    }

//////////////////////////////////////////////////////////////////////////////
// instance methods
//

    // create
    public BloodPressureModel(ObservationSource source, Integer systolic, Integer diastolic,
                              Date readingDate, Boolean followedProtocol,
                              FhirConfigManager fcm) throws DataException {

        super(source, followedProtocol, readingDate);

        if (systolic == null && diastolic == null) {
            throw new DataException("systolic and diastolic are null (at least one must be provided)");
        }

        if (systolic != null) {
            this.systolic = new QuantityModel(systolic, fcm.getBpValueUnit());
        }

        if (diastolic != null) {
            this.diastolic = new QuantityModel(diastolic, fcm.getBpValueUnit());
        }

        this.readingDate = readingDate; //.getTime();
    }

    // read local
    public BloodPressureModel(HomeBloodPressureReading reading, FhirConfigManager fcm) throws DataException {
        super(ObservationSource.valueOf(reading.getSource()), reading.getFollowedInstructions(), reading.getReadingDate());

        if (reading.getSystolic() == null && reading.getDiastolic() == null) {
            throw new DataException("systolic and diastolic are null (at least one must be provided)");
        }

        if (reading.getSystolic() != null) {
            systolic = new QuantityModel(reading.getSystolic(), fcm.getBpValueUnit());
        }

        if (reading.getDiastolic() != null) {
            diastolic = new QuantityModel(reading.getDiastolic(), fcm.getBpValueUnit());
        }

        readingDate = reading.getReadingDate(); //.getTime();
    }

    // read remote, no encounter reference or resource available
    public BloodPressureModel(Observation bpObservation, FhirConfigManager fcm) throws DataException {
        super(ObservationUtil.getBPSource(bpObservation, fcm), bpObservation, null, fcm);

        buildFromBPObservation(bpObservation, fcm);
    }

    // read remote, has encounter and possibly protocol
    public BloodPressureModel(Encounter encounter, Observation bpObservation,
                              Observation protocolObservation, FhirConfigManager fcm) throws DataException {

        super(encounter, ObservationUtil.getBPSource(bpObservation, encounter, fcm), bpObservation, protocolObservation, fcm);

        buildFromBPObservation(bpObservation, fcm);
    }

    private void buildFromBPObservation(Observation bpObservation, FhirConfigManager fcm) {
        this.sourceBPObservation = bpObservation;

        // todo : set id.  but to what?  first Observation's id?  what about the others?  how is id used?  do we need
        //        to retain the ids for the Encounter and other Observations?

        CodeableConcept code = bpObservation.getCode();
        if (FhirUtil.hasCoding(code, fcm.getBpSystolicCoding()) || FhirUtil.hasCoding(code, fcm.getBpHomeBluetoothSystolicCoding())) {
            systolic = new QuantityModel(bpObservation.getValueQuantity());

        } else if (FhirUtil.hasCoding(code, fcm.getBpDiastolicCoding()) || FhirUtil.hasCoding(code, fcm.getBpHomeBluetoothDiastolicCoding())) {
            diastolic = new QuantityModel(bpObservation.getValueQuantity());

        } else { // it's not a raw systolic or diastolic reading of any sort, so it must be a panel.  right?
            for (Observation.ObservationComponentComponent occ : bpObservation.getComponent()) {
                ValueType valueType = ValueType.UNKNOWN;

                CodeableConcept cc = occ.getCode();
                for (Coding c : cc.getCoding()) {
                    if (FhirUtil.matches(c, fcm.getBpSystolicCoding()) || FhirUtil.matches(c, fcm.getBpHomeBluetoothSystolicCoding())) {
                        valueType = ValueType.SYSTOLIC;
                        break;

                    } else if (FhirUtil.matches(c, fcm.getBpDiastolicCoding()) || FhirUtil.matches(c, fcm.getBpHomeBluetoothDiastolicCoding())) {
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
        }
    }

    public boolean isHomeReading() {
        return source == ObservationSource.HOME || source == ObservationSource.HOME_BLUETOOTH;
    }

    @Override
    public String getReadingType() {
        if (systolic != null && diastolic == null) {
            return "Systolic";

        } else if (systolic == null && diastolic != null) {
            return "Diastolic";

        } else if (systolic != null && diastolic != null) {
            return "BP Panel";

        } else {    // shouldn't ever get here
            return "(n/a)";
        }
    }

    @Override
    public String getValue() {
        if (systolic != null && diastolic != null) {
            return systolic.getValue() + "/" + diastolic.getValue() + " " + systolic.getUnit();

        } else if (systolic != null) {
            return systolic.getValue() + " " + systolic.getUnit();

        } else if (diastolic != null) {
            return diastolic.getValue() + " " + diastolic.getUnit();

        } else {    // shouldn't ever get here
            return "(n/a)";
        }
    }

    @JsonIgnore
    public Observation getSourceBPObservation() {
        return sourceBPObservation;
    }

    public QuantityModel getSystolic() {
        return systolic;
    }

    public QuantityModel getDiastolic() {
        return diastolic;
    }
}
