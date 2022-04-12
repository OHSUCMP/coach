package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.EncounterMatcher;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public abstract class AbstractVitalsModel extends AbstractModel implements Comparable<AbstractVitalsModel> {
    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

    public enum Source {
        OFFICE,
        HOME,
        UNKNOWN
    }

    protected Encounter sourceEncounter;
    protected Observation sourceProtocolObservation;

    protected Source source;
    protected Boolean followedProtocol;
    protected Date readingDate;

    public AbstractVitalsModel(Source source, Boolean followedProtocol, Date readingDate) {
        this.source = source;
        this.followedProtocol = followedProtocol;
        this.readingDate = readingDate;
    }

    public AbstractVitalsModel(Encounter enc, Observation protocolObservation,
                               Observation readingDateObservation, FhirConfigManager fcm) throws DataException {
        this.sourceEncounter = enc;
        this.sourceProtocolObservation = protocolObservation;

        EncounterMatcher matcher = new EncounterMatcher(fcm);
        if (matcher.isAmbEncounter(enc))              source = Source.OFFICE;
        else if (matcher.isHomeHealthEncounter(enc))  source = Source.HOME;
        else                                          source = Source.UNKNOWN;

        if (protocolObservation != null &&
                protocolObservation.getCode().hasCoding(fcm.getProtocolSystem(), fcm.getProtocolCode()) &&
                protocolObservation.hasValueCodeableConcept() &&
                protocolObservation.getValueCodeableConcept().hasCoding(fcm.getProtocolAnswerSystem(), fcm.getProtocolAnswerCode()) &&
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

        if (readingDateObservation.getEffectiveDateTimeType() != null) {
            this.readingDate = readingDateObservation.getEffectiveDateTimeType().getValue(); //.getTime();

        } else if (readingDateObservation.getEffectiveInstantType() != null) {
            this.readingDate = readingDateObservation.getEffectiveInstantType().getValue(); //.getTime();

        } else if (readingDateObservation.getEffectivePeriod() != null) {
            this.readingDate = readingDateObservation.getEffectivePeriod().getEnd(); //.getTime();

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

    @JsonIgnore
    public Observation getSourceProtocolObservation() {
        return sourceProtocolObservation;
    }

    public Source getSource() {
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
        o.getCode().addCoding()
                .setCode(fcm.getProtocolCode())
                .setSystem(fcm.getProtocolSystem())
                .setDisplay(fcm.getProtocolDisplay());

        addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(readingDate));

        String answerValue = followedProtocol ?
                fcm.getProtocolAnswerYes() :
                fcm.getProtocolAnswerNo();

        o.setValue(new CodeableConcept());
        o.getValueCodeableConcept()
                .setText(answerValue)
                .addCoding()
                .setCode(fcm.getProtocolAnswerCode())
                .setSystem(fcm.getProtocolAnswerSystem())
                .setDisplay(fcm.getProtocolAnswerDisplay());

        return o;
    }

    protected void addHomeSettingExtension(DomainResource domainResource) {
        // setting MeasurementSettingExt to indicate taken in a "home" setting
        // see https://browser.ihtsdotools.org/?perspective=full&conceptId1=264362003&edition=MAIN/SNOMEDCT-US/2021-09-01&release=&languages=en

        domainResource.addExtension(new Extension()
                .setUrl("http://hl7.org/fhir/us/vitals/StructureDefinition/MeasurementSettingExt")
                .setValue(new Coding()
                        .setCode("264362003")
                        .setSystem("http://snomed.info/sct")));
    }

    protected String genTemporaryId() {
        return UUID.randomUUID().toString();
    }

}
