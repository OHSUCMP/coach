package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.HomePulseReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.ObservationUtil;
import org.hl7.fhir.r4.model.*;

import java.util.Date;

public class PulseModel extends AbstractVitalsModel {
    private Observation sourcePulseObservation;

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

        pulse = new QuantityModel(reading.getPulse(), fcm.getPulseValueUnit());
        readingDate = reading.getReadingDate(); //.getTime();
    }

    // read remote, no encounter reference or resource available
    public PulseModel(Observation pulseObservation, FhirConfigManager fcm) throws DataException {
        super(ObservationUtil.getPulseSource(pulseObservation), null, ObservationUtil.getReadingDate(pulseObservation), fcm);

        buildFromPulseObservation(pulseObservation);
    }

    // read remote
    public PulseModel(Encounter enc, Observation pulseObservation,
                      Observation protocolObservation, FhirConfigManager fcm) throws DataException {

        super(enc, ObservationUtil.getSourceByEncounter(enc, fcm), protocolObservation, ObservationUtil.getReadingDate(pulseObservation), fcm);

        buildFromPulseObservation(pulseObservation);
    }

    private void buildFromPulseObservation(Observation pulseObservation) {
        this.sourcePulseObservation = pulseObservation;
        this.pulse = new QuantityModel(pulseObservation.getValueQuantity());
    }

    @Override
    public String getReadingType() {
        return "Pulse";
    }

    @Override
    public String getValue() {
        return pulse.getValue() + " " + pulse.getUnit();
    }

    @JsonIgnore
    public Observation getSourcePulseObservation() {
        return sourcePulseObservation;
    }

    public QuantityModel getPulse() {
        return pulse;
    }
}
