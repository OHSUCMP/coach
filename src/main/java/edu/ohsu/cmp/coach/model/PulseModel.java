package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.HomePulseReading;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.util.ObservationUtil;
import org.hl7.fhir.r4.model.*;

import java.util.Date;

public class PulseModel extends AbstractVitalsModel implements FHIRCompatible {
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

    // read remote
    public PulseModel(Encounter enc, Observation pulseObservation,
                      Observation protocolObservation, FhirConfigManager fcm) throws DataException {

        super(ObservationUtil.getSourceByEncounter(enc, fcm), pulseObservation, protocolObservation, fcm);

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

    @Override
    public Bundle toBundle(String patientId, FhirConfigManager fcm) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (sourcePulseObservation != null) {
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourcePulseObservation));
            if (sourceProtocolObservation != null) {
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourceProtocolObservation));
            }

        } else {
            // Logica doesn't handle absolute URLs in references well.  it's possible other FHIR server
            // implementations don't handle them well either.
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

            Encounter enc = buildNewHomeHealthEncounter(fcm, patientIdRef);
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(enc));

            Observation pulseObservation = buildPulseObservation(patientIdRef, enc, fcm);
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(pulseObservation));

            if (followedProtocol != null && followedProtocol) {
                Observation protocolObservation = buildProtocolObservation(patientIdRef, enc, fcm);
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(protocolObservation));
            }
        }

        return bundle;
    }

    private Observation buildPulseObservation(String patientId, Encounter enc, FhirConfigManager fcm) {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        o.setEncounter(new Reference().setReference(URN_UUID + enc.getId()));

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        o.getCode().addCoding(fcm.getPulseCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(readingDate));

        o.setValue(new Quantity());
        o.getValueQuantity()
                .setCode(fcm.getPulseValueCode())
                .setSystem(fcm.getPulseValueSystem())
                .setUnit(fcm.getPulseValueUnit())
                .setValue(pulse.getValue().intValue());

        return o;
    }
}
