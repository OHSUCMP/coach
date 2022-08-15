package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public abstract class AbstractVitalsModel extends AbstractModel implements Comparable<AbstractVitalsModel> {
    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Observation sourceProtocolObservation;

    protected ObservationSource source = null;
    protected Boolean followedProtocol;
    protected Date readingDate;

    public AbstractVitalsModel(ObservationSource source, Boolean followedProtocol, Date readingDate) {
        this.source = source;
        this.followedProtocol = followedProtocol;
        this.readingDate = readingDate;
    }

    public AbstractVitalsModel(ObservationSource source, Observation observation,
                               Observation protocolObservation, FhirConfigManager fcm) throws DataException {
        this.source = source;
        this.sourceProtocolObservation = protocolObservation;

        if (source == ObservationSource.HOME && ! FhirUtil.hasHomeSettingExtension(observation)) {
            logger.warn("Observation " + observation.getId() + " has HOME Encounter but is missing the Home Setting Extension");
        }

        if (protocolObservation != null &&
                FhirUtil.hasCoding(protocolObservation.getCode(), fcm.getProtocolCoding()) &&
                protocolObservation.hasValueCodeableConcept() &&
                FhirUtil.hasCoding(protocolObservation.getValueCodeableConcept(), fcm.getProtocolAnswerCoding()) &&
                protocolObservation.getValueCodeableConcept().hasText()) {

            String answerValue = protocolObservation.getValueCodeableConcept().getText();

            if (answerValue.equals(fcm.getProtocolAnswerYes())) {
                this.followedProtocol = true;

            } else if (answerValue.equals(fcm.getProtocolAnswerNo())) {
                this.followedProtocol = false;

            } else {
                throw new CaseNotHandledException("couldn't handle case where protocol answer='" + answerValue + "'");
            }
        }

        if (observation.getEffectiveDateTimeType() != null) {
            this.readingDate = observation.getEffectiveDateTimeType().getValue(); //.getTime();

        } else if (observation.getEffectiveInstantType() != null) {
            this.readingDate = observation.getEffectiveInstantType().getValue(); //.getTime();

        } else if (observation.getEffectivePeriod() != null) {
            this.readingDate = observation.getEffectivePeriod().getEnd(); //.getTime();

        } else {
            throw new DataException("missing timestamp");
        }
    }

    @Override
    public int compareTo(@NotNull AbstractVitalsModel o) {
        return readingDate.compareTo(o.readingDate);
    }

    @JsonIgnore
    public Observation getSourceProtocolObservation() {
        return sourceProtocolObservation;
    }

    public ObservationSource getSource() {
        return source;
    }

    public Boolean getFollowedProtocol() {
        return followedProtocol;
    }

    public Date getReadingDate() {
        return readingDate;
    }

    public String getReadingDateString() {
        return DATE_FORMAT.format(readingDate);
    }

    public Long getReadingDateTimestamp() {
        return readingDate.getTime();
    }

    public abstract String getReadingType();
    
    public abstract String getValue();

//////////////////////////////////////////////////////////////////////////////
// private methods
//
    protected Encounter buildNewHomeHealthEncounter(FhirConfigManager fcm, String patientId) {
        Encounter e = new Encounter();

        e.setId(genTemporaryId());

        e.setStatus(Encounter.EncounterStatus.FINISHED);

        e.getClass_().setSystem(fcm.getEncounterClassSystem())
                .setCode(fcm.getEncounterClassHHCode())
                .setDisplay(fcm.getEncounterClassHHDisplay());

        e.setSubject(new Reference().setReference(patientId));

        Calendar start = Calendar.getInstance();
        start.setTime(readingDate);
        start.add(Calendar.MINUTE, -1);

        Calendar end = Calendar.getInstance();
        end.setTime(readingDate);
        end.add(Calendar.MINUTE, 1);

        e.getPeriod().setStart(start.getTime()).setEnd(end.getTime());

        return e;
    }

    protected Observation buildProtocolObservation(String patientId, Encounter enc, FhirConfigManager fcm) {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        o.setEncounter(new Reference().setReference(URN_UUID + enc.getId()));

        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding(fcm.getProtocolCoding());

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(readingDate));

        String answerValue = followedProtocol ?
                fcm.getProtocolAnswerYes() :
                fcm.getProtocolAnswerNo();

        o.setValue(new CodeableConcept());
        o.getValueCodeableConcept()
                .setText(answerValue)
                .addCoding(fcm.getProtocolAnswerCoding());

        return o;
    }

    protected String genTemporaryId() {
        return UUID.randomUUID().toString();
    }
}
