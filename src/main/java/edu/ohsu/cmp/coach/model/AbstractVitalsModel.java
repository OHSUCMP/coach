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
import java.util.Date;

public abstract class AbstractVitalsModel extends AbstractModel implements Comparable<AbstractVitalsModel> {
//    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
//    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

//    protected static final String PROTOCOL_NOTE_TAG = "COACH_PROTOCOL::";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Encounter sourceEncounter;
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
        this(null, source, observation, protocolObservation, fcm);
    }

    public AbstractVitalsModel(Encounter encounter, ObservationSource source, Observation observation,
                               Observation protocolObservation, FhirConfigManager fcm) throws DataException {
        this.sourceEncounter = encounter;
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
    public Encounter getSourceEncounter() {
        return sourceEncounter;
    }

    // implementing setSourceEncounter to facilitate the creation of new resources that use Encounter
    @JsonIgnore
    public void setSourceEncounter(Encounter sourceEncounter) {
        this.sourceEncounter = sourceEncounter;
    }

    @JsonIgnore
    public Observation getSourceProtocolObservation() {
        return sourceProtocolObservation;
    }

    public void setSourceProtocolObservation(Observation sourceProtocolObservation) {
        this.sourceProtocolObservation = sourceProtocolObservation;
    }

    public ObservationSource getSource() {
        return source;
    }

    public Boolean getFollowedProtocol() {
        return followedProtocol;
    }

    public void setFollowedProtocol(Boolean followedProtocol) {
        this.followedProtocol = followedProtocol;
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
}
