package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.util.ObservationUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class BloodPressureModel extends AbstractVitalsModel {
    private Observation sourceBPObservation = null;
    private Observation sourceSystolicObservation = null;
    private Observation sourceDiastolicObservation = null;

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

        if (systolic == null || diastolic == null) {
            throw new DataException("both systolic and diastolic required");
        }

        this.systolic = new QuantityModel(systolic, fcm.getBpValueUnit());
        this.diastolic = new QuantityModel(diastolic, fcm.getBpValueUnit());
        this.readingDate = readingDate; //.getTime();
    }

    // read local
    public BloodPressureModel(HomeBloodPressureReading reading, FhirConfigManager fcm) throws DataException {
        super(ObservationSource.valueOf(reading.getSource()), reading.getFollowedInstructions(), reading.getReadingDate());

        if (reading.getSystolic() == null || reading.getDiastolic() == null) {
            throw new DataException("both systolic and diastolic are required");
        }

        systolic = new QuantityModel(reading.getSystolic(), fcm.getBpValueUnit());
        diastolic = new QuantityModel(reading.getDiastolic(), fcm.getBpValueUnit());
        readingDate = reading.getReadingDate(); //.getTime();
    }

    // read remote, no encounter reference or resource available
    public BloodPressureModel(Observation bpObservation, FhirConfigManager fcm) throws DataException {
        super(ObservationUtil.getBPSource(bpObservation, fcm), null, ObservationUtil.getReadingDate(bpObservation), fcm);

        buildFromBPObservation(bpObservation, fcm);
    }

    // read remote, has encounter and possibly protocol
    public BloodPressureModel(Encounter encounter, Observation bpObservation,
                              Observation protocolObservation, FhirConfigManager fcm) throws DataException {

        super(encounter, ObservationUtil.getBPSource(bpObservation, encounter, fcm), protocolObservation, ObservationUtil.getReadingDate(bpObservation), fcm);

        buildFromBPObservation(bpObservation, fcm);
    }

    public BloodPressureModel(Encounter encounter, Observation systolicObservation, Observation diastolicObservation,
                              Observation protocolObservation, FhirConfigManager fcm) throws DataException {

        super(encounter, ObservationUtil.getBPSource(systolicObservation, encounter, fcm), protocolObservation, ObservationUtil.getReadingDate(systolicObservation), fcm);

        buildFromSystolicDiastolicObservations(systolicObservation, diastolicObservation, fcm);
    }

    private void buildFromBPObservation(Observation bpObservation, FhirConfigManager fcm) throws DataException {
        this.sourceBPObservation = bpObservation;

        CodeableConcept code = bpObservation.getCode();
        if (FhirUtil.hasCoding(code, fcm.getBpPanelCodings())) {
            for (Observation.ObservationComponentComponent occ : bpObservation.getComponent()) {
                CodeableConcept cc = occ.getCode();
                if (FhirUtil.hasCoding(cc, fcm.getSystolicCodings())) {
                    systolic = new QuantityModel(occ.getValueQuantity(), fcm.getBpValueUnit());

                } else if (FhirUtil.hasCoding(cc, fcm.getDiastolicCodings())) {
                    diastolic = new QuantityModel(occ.getValueQuantity(), fcm.getBpValueUnit());
                }
            }

            if (systolic == null || diastolic == null) {
                throw new DataException("both systolic and diastolic required");
            }

        } else {
            throw new DataException("only BP panel Observations permitted in this context");
        }
    }

    // read remote, no encounter reference or resource available
    // systolic and diastolic are split across two separate Observations
    public BloodPressureModel(Observation systolicObservation, Observation diastolicObservation, FhirConfigManager fcm) throws DataException {
        super(ObservationUtil.getBPSource(systolicObservation, fcm), null, ObservationUtil.getReadingDate(systolicObservation), fcm);

        buildFromSystolicDiastolicObservations(systolicObservation, diastolicObservation, fcm);
    }

    private void buildFromSystolicDiastolicObservations(Observation systolicObservation, Observation diastolicObservation, FhirConfigManager fcm) throws DataException {
        this.sourceSystolicObservation = systolicObservation;
        this.sourceDiastolicObservation = diastolicObservation;

        if (systolicObservation == null || diastolicObservation == null) {
            throw new DataException("both systolic and diastolic Observations are required");
        }

        // todo : set id.  but to what?  first Observation's id?  what about the others?  how is id used?  do we need
        //        to retain the ids for the Encounter and other Observations?

        if (systolicObservation.hasCode() && FhirUtil.hasCoding(systolicObservation.getCode(), fcm.getSystolicCodings())) {
            systolic = new QuantityModel(systolicObservation.getValueQuantity(), fcm.getBpValueUnit());
            if (StringUtils.isEmpty(systolic.getUnit())) {
                systolic.setUnit(fcm.getBpValueUnit());
            }
        } else {
            throw new DataException("systolic observation : invalid coding");
        }

        if (diastolicObservation.hasCode() && FhirUtil.hasCoding(diastolicObservation.getCode(), fcm.getDiastolicCodings())) {
            diastolic = new QuantityModel(diastolicObservation.getValueQuantity(), fcm.getBpValueUnit());
            if (StringUtils.isEmpty(diastolic.getUnit())) {
                diastolic.setUnit(fcm.getBpValueUnit());
            }
        } else {
            throw new DataException("diastolic observation : invalid coding");
        }
    }

    public boolean isHomeReading() {
        return source == ObservationSource.HOME || source == ObservationSource.HOME_BLUETOOTH;
    }

    @Override
    public String getReadingType() {
        return systolic != null && diastolic != null ?
                "BP Panel" :
                "(n/a)";        // shouldn't ever get here
    }

    @Override
    public String getValue() {
        return systolic != null && diastolic != null ?
                systolic.getValue() + "/" + diastolic.getValue() + " " + systolic.getUnit() :
                "(n/a)";        // shouldn't ever get here
    }


    @Override
    public String getLogicalEqualityKey() {
        return "BP" + KEY_DELIM +
                systolic.getValue().intValue() + KEY_DELIM +
                diastolic.getValue().intValue() + KEY_DELIM +
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        ZonedDateTime.ofInstant(readingDate.toInstant(), ZoneOffset.UTC)
                );
    }

    @JsonIgnore
    public Observation getSourceBPObservation() {
        return sourceBPObservation;
    }

    @JsonIgnore
    public Observation getSourceSystolicObservation() {
        return sourceSystolicObservation;
    }

    @JsonIgnore
    public Observation getSourceDiastolicObservation() {
        return sourceDiastolicObservation;
    }

    public QuantityModel getSystolic() {
        return systolic;
    }

    public QuantityModel getDiastolic() {
        return diastolic;
    }
}
