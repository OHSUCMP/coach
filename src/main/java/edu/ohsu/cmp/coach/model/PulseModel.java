package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.HomePulseReading;
import edu.ohsu.cmp.coach.entity.MyOmronVitals;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.omron.OmronBloodPressureModel;
import edu.ohsu.cmp.coach.util.ObservationUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PulseModel extends AbstractVitalsModel {
    private Long localDatabaseId = null;
    private Observation sourcePulseObservation = null;

    private OmronBloodPressureModel sourceOmronBloodPressureModel = null;

    private QuantityModel pulse;

//////////////////////////////////////////////////////////////////////////////
// instance methods
//

    // create
    public PulseModel(ObservationSource source, Integer pulse,
                      Date readingDate, Boolean followedProtocol,
                      FhirConfigManager fcm) {

        super(source, followedProtocol, readingDate);

        this.pulse = new QuantityModel(pulse, fcm.getPulseValueUnit());
    }

    // read local
    public PulseModel(HomePulseReading reading, FhirConfigManager fcm) {
        super(ObservationSource.HOME, reading.getFollowedInstructions(), reading.getReadingDate());

        localDatabaseId = reading.getId();
        pulse = new QuantityModel(reading.getPulse(), fcm.getPulseValueUnit());
        readingDate = reading.getReadingDate(); //.getTime();
    }

    // read remote, no encounter reference or resource available
    public PulseModel(Observation pulseObservation, FhirConfigManager fcm) throws DataException {
        super(ObservationUtil.getPulseSource(pulseObservation), null, ObservationUtil.getReadingDate(pulseObservation), fcm);

        buildFromPulseObservation(pulseObservation, fcm);
    }

    // read remote
    public PulseModel(Encounter enc, Observation pulseObservation,
                      Observation protocolObservation, FhirConfigManager fcm) throws DataException {

        super(enc, ObservationUtil.getSourceByEncounter(enc, fcm), protocolObservation, ObservationUtil.getReadingDate(pulseObservation), fcm);

        buildFromPulseObservation(pulseObservation, fcm);
    }

    private void buildFromPulseObservation(Observation pulseObservation, FhirConfigManager fcm) throws DataException {
        this.sourcePulseObservation = pulseObservation;
        this.pulse = new QuantityModel(pulseObservation.getValueQuantity(), fcm.getPulseValueUnit());
        if (StringUtils.isEmpty(pulse.getUnit())) { // Epic doesn't use units so this will be null for Epic flowsheet-based data
            pulse.setUnit(fcm.getPulseValueUnit());
        }
    }

    public PulseModel(OmronBloodPressureModel model, FhirConfigManager fcm) throws ParseException {
        super(ObservationSource.HOME, null, OMRON_DATETIME_FORMAT.parse(model.getDateTimeLocal() + model.getDateTimeUtcOffset()), fcm);
        sourceOmronBloodPressureModel = model;
        pulse = new QuantityModel(model.getPulse(), model.getPulseUnits());
    }

    public PulseModel(MyOmronVitals vitals, FhirConfigManager fcm) throws ParseException {
        super(ObservationSource.HOME, null, OMRON_DATETIME_FORMAT.parse(vitals.getDateTimeLocal() + vitals.getDateTimeUtcOffset()), fcm);
        sourceOmronBloodPressureModel = new OmronBloodPressureModel(vitals);
        localDatabaseId = vitals.getId();
        pulse = new QuantityModel(vitals.getPulse(), vitals.getPulseUnits());
    }

    @Override
    public String getReadingType() {
        return "Pulse";
    }

    @Override
    public String getValue() {
        return pulse.getValue() + " " + pulse.getUnit();
    }

    @Override
    public String getLogicalEqualityKey() {
        return "P" + KEY_DELIM +
                pulse.getValue().intValue() + KEY_DELIM +
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                        ZonedDateTime.ofInstant(readingDate.toInstant(), ZoneOffset.UTC)
                );
    }

    @JsonIgnore
    public Observation getSourcePulseObservation() {
        return sourcePulseObservation;
    }

    public QuantityModel getPulse() {
        return pulse;
    }

    @Override
    public String toString() {
        if (sourcePulseObservation != null) {
            return "remote-" + sourcePulseObservation.getId();
        } else if (sourceOmronBloodPressureModel != null) {
            return "omron-" + sourceOmronBloodPressureModel.getId();
        } else if (localDatabaseId != null) {
            return "local-" + localDatabaseId;
        } else {
            return pulse + " (id unknown)";
        }
    }
}
