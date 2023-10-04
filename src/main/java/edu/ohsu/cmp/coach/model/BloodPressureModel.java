package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.entity.MyOmronVitals;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.omron.OmronBloodPressureModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.util.ObservationUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class BloodPressureModel extends AbstractVitalsModel {
    private Long localDatabaseId = null;
    private Observation sourceBPObservation = null;
    private Observation sourceSystolicObservation = null;
    private Observation sourceDiastolicObservation = null;
    private OmronBloodPressureModel sourceOmronBloodPressureModel = null;

    private QuantityModel systolic = null;
    private QuantityModel diastolic = null;


//////////////////////////////////////////////////////////////////////////////
// instance methods
//

    // create
    public BloodPressureModel(ObservationSource source, Integer systolic, Integer diastolic,
                              Date readingDate, Boolean followedProtocol,
                              FhirConfigManager fcm) throws DataException {

        super(source, followedProtocol, readingDate);

        if (systolic == null || diastolic == null) {
            throw new DataException("both systolic and diastolic required (manual method)");
        }

        this.systolic = new QuantityModel(systolic, fcm.getBpValueUnit());
        this.diastolic = new QuantityModel(diastolic, fcm.getBpValueUnit());
        this.readingDate = readingDate; //.getTime();
    }

    // read local
    public BloodPressureModel(HomeBloodPressureReading reading, FhirConfigManager fcm) throws DataException {
        super(ObservationSource.HOME, reading.getFollowedInstructions(), reading.getReadingDate());

        if (reading.getSystolic() == null || reading.getDiastolic() == null) {
            throw new DataException("both systolic and diastolic are required (reading.id=" + reading.getId() + ")");
        }

        localDatabaseId = reading.getId();
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
        sourceBPObservation = bpObservation;

        CodeableConcept code = bpObservation.getCode();
        if (FhirUtil.hasCoding(code, fcm.getBpPanelCodings())) {
            for (Observation.ObservationComponentComponent occ : bpObservation.getComponent()) {
                CodeableConcept cc = occ.getCode();
                if (FhirUtil.hasCoding(cc, fcm.getBpSystolicCodings())) {
                    systolic = new QuantityModel(occ.getValueQuantity(), fcm.getBpValueUnit());

                } else if (FhirUtil.hasCoding(cc, fcm.getBpDiastolicCodings())) {
                    diastolic = new QuantityModel(occ.getValueQuantity(), fcm.getBpValueUnit());
                }
            }

            if (systolic == null || diastolic == null) {
                throw new DataException("both systolic and diastolic required (Observation.id=" + bpObservation.getId() + ")");
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

    public BloodPressureModel(MyOmronVitals vitals, FhirConfigManager fcm) throws ParseException, DataException {
        super(ObservationSource.HOME, null, OMRON_DATETIME_FORMAT.parse(vitals.getDateTimeLocal() + vitals.getDateTimeUtcOffset()), fcm);
        sourceOmronBloodPressureModel = new OmronBloodPressureModel(vitals);
        localDatabaseId = vitals.getId();
        systolic = new QuantityModel(vitals.getSystolic(), vitals.getBloodPressureUnits());
        diastolic = new QuantityModel(vitals.getDiastolic(), vitals.getBloodPressureUnits());
    }

    private void buildFromSystolicDiastolicObservations(Observation systolicObservation, Observation diastolicObservation, FhirConfigManager fcm) throws DataException {
        sourceSystolicObservation = systolicObservation;
        sourceDiastolicObservation = diastolicObservation;

        if (systolicObservation == null || diastolicObservation == null) {
            throw new DataException("both systolic and diastolic Observations are required");
        }

        // todo : set id.  but to what?  first Observation's id?  what about the others?  how is id used?  do we need
        //        to retain the ids for the Encounter and other Observations?

        if (systolicObservation.hasCode() && FhirUtil.hasCoding(systolicObservation.getCode(), fcm.getBpSystolicCodings())) {
            systolic = new QuantityModel(systolicObservation.getValueQuantity(), fcm.getBpValueUnit());
            if (StringUtils.isEmpty(systolic.getUnit())) {
                systolic.setUnit(fcm.getBpValueUnit());
            }
        } else {
            throw new DataException("systolic observation : invalid coding (Observation.id=" + systolicObservation.getId() + ")");
        }

        if (diastolicObservation.hasCode() && FhirUtil.hasCoding(diastolicObservation.getCode(), fcm.getBpDiastolicCodings())) {
            diastolic = new QuantityModel(diastolicObservation.getValueQuantity(), fcm.getBpValueUnit());
            if (StringUtils.isEmpty(diastolic.getUnit())) {
                diastolic.setUnit(fcm.getBpValueUnit());
            }
        } else {
            throw new DataException("diastolic observation : invalid coding (Observation.id=" + diastolicObservation.getId() + ")");
        }
    }

    public boolean isHomeReading() {
        return source == ObservationSource.HOME;
    }

    @Override
    public String toString() {
        if (sourceBPObservation != null) {
            return "remote-panel-" + sourceBPObservation.getId();
        } else if (sourceSystolicObservation != null) {
            return "remote-systolic-" + sourceSystolicObservation.getId();
        } else if (sourceOmronBloodPressureModel != null) {
            return "omron-" + sourceOmronBloodPressureModel.getId();
        } else if (localDatabaseId != null) {
            return "local-" + localDatabaseId;
        } else {
            return systolic + "/" + diastolic + " (id unknown)";
        }
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
