package edu.ohsu.cmp.coach.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.fhir.EncounterMatcher;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class BloodPressureModel implements FHIRCompatible {
    public static final String OBSERVATION_CATEGORY_SYSTEM = "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String OBSERVATION_CATEGORY_CODE = "vital-signs";

    public static final String URN_UUID = "urn:uuid:";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yy h:mm a");

    private Encounter sourceEncounter;
    private Observation sourceBPObservation;
    private Observation sourcePulseObservation;
    private Observation sourceProtocolObservation;

    private Source source;
    private QuantityModel systolic;
    private QuantityModel diastolic;
    private QuantityModel pulse;
    private Date readingDate;
    private Boolean followedProtocol;

    public enum Source {
        OFFICE,
        HOME,
        UNKNOWN
    }

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
    public BloodPressureModel(Source source, Integer systolic, Integer diastolic, Integer pulse,
                              Date readingDate, Boolean followedProtocol,
                              FhirConfigManager fcm) {

        this.source = source;
        this.systolic = new QuantityModel(systolic, fcm.getBpValueUnit());
        this.diastolic = new QuantityModel(diastolic, fcm.getBpValueUnit());
        this.pulse = new QuantityModel(pulse, fcm.getPulseValueUnit());
        this.readingDate = readingDate; //.getTime();
        this.followedProtocol = followedProtocol;
    }

    // read local
    public BloodPressureModel(HomeBloodPressureReading reading, FhirConfigManager fcm) {
        source = Source.HOME;
        systolic = new QuantityModel(reading.getSystolic(), fcm.getBpValueUnit());
        diastolic = new QuantityModel(reading.getDiastolic(), fcm.getBpValueUnit());
        pulse = new QuantityModel(reading.getPulse(), fcm.getPulseValueUnit());
        readingDate = reading.getReadingDate(); //.getTime();
        followedProtocol = reading.getFollowedInstructions();
    }

    // read remote
    public BloodPressureModel(Encounter enc, Observation bpObservation,
                              Observation pulseObservation, Observation protocolObservation,
                              FhirConfigManager fcm) throws DataException {

        this.sourceEncounter = enc;
        this.sourceBPObservation = bpObservation;
        this.sourcePulseObservation = pulseObservation;
        this.sourceProtocolObservation = protocolObservation;

        // todo : set id.  but to what?  first Observation's id?  what about the others?  how is id used?  do we need
        //        to retain the ids for the Encounter and other Observations?

        EncounterMatcher matcher = new EncounterMatcher(fcm);
        if (matcher.isAmbEncounter(enc))              source = Source.OFFICE;
        else if (matcher.isHomeHealthEncounter(enc))  source = Source.HOME;
        else                                          source = Source.UNKNOWN;

        for (Observation.ObservationComponentComponent occ : bpObservation.getComponent()) {
            ValueType valueType = ValueType.UNKNOWN;

            CodeableConcept cc = occ.getCode();
            for (Coding c : cc.getCoding()) {
                if (c.getSystem().equals(fcm.getBpSystem()) && c.getCode().equals(fcm.getBpSystolicCode())) {
                    valueType = ValueType.SYSTOLIC;
                    break;

                } else if (c.getSystem().equals(fcm.getBpSystem()) && c.getCode().equals(fcm.getBpDiastolicCode())) {
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

        if (bpObservation.getEffectiveDateTimeType() != null) {
            this.readingDate = bpObservation.getEffectiveDateTimeType().getValue(); //.getTime();

        } else if (bpObservation.getEffectiveInstantType() != null) {
            this.readingDate = bpObservation.getEffectiveInstantType().getValue(); //.getTime();

        } else if (bpObservation.getEffectivePeriod() != null) {
            this.readingDate = bpObservation.getEffectivePeriod().getEnd(); //.getTime();

        } else {
            throw new DataException("missing timestamp");
        }

        if (pulseObservation != null &&
                pulseObservation.getCode().hasCoding(fcm.getPulseSystem(), fcm.getPulseCode()) &&
                pulseObservation.hasValueQuantity()) {

            this.pulse = new QuantityModel(pulseObservation.getValueQuantity());
        }

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
    }

    @JsonIgnore
    public Encounter getSourceEncounter() {
        return sourceEncounter;
    }

    @JsonIgnore
    public Observation getSourceBPObservation() {
        return sourceBPObservation;
    }

    @JsonIgnore
    public Observation getSourcePulseObservation() {
        return sourcePulseObservation;
    }

    @JsonIgnore
    public Observation getSourceProtocolObservation() {
        return sourceProtocolObservation;
    }

    public Source getSource() {
        return source;
    }

    public QuantityModel getSystolic() {
        return systolic;
    }

    public QuantityModel getDiastolic() {
        return diastolic;
    }

    public boolean hasPulse() {
        return pulse != null;
    }

    public QuantityModel getPulse() {
        return pulse;
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

    public Boolean getFollowedProtocol() {
        return followedProtocol;
    }

    @Override
    public Bundle toBundle(String patientId, FhirConfigManager fcm) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (sourceEncounter != null && sourceBPObservation != null) {
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourceEncounter));
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourceBPObservation));
            if (sourcePulseObservation != null) {
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourcePulseObservation));
            }
            if (sourceProtocolObservation != null) {
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourceProtocolObservation));
            }

        } else {
            // Logica doesn't handle absolute URLs in references well.  it's possible other FHIR server
            // implementations don't handle them well either.
            String patientIdRef = FhirUtil.toRelativeReference(patientId);

            Encounter enc = buildNewHomeHealthEncounter(fcm, patientIdRef);
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(enc));

            Observation bpObservation = buildHomeHealthBloodPressureObservation(patientIdRef, enc, fcm);
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(bpObservation));

            if (pulse != null) {
                Observation pulseObservation = buildPulseObservation(patientIdRef, enc, fcm);
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(pulseObservation));
            }

            if (followedProtocol != null && followedProtocol) {
                Observation protocolObservation = buildProtocolObservation(patientIdRef, enc, fcm);
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(protocolObservation));
            }
        }

        return bundle;
    }


//////////////////////////////////////////////////////////////////////////////
// private methods
//
    private Encounter buildNewHomeHealthEncounter(FhirConfigManager fcm, String patientId) {
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

//    adapted from CDSHooksExecutor.buildHomeBloodPressureObservation()
//    used when creating new Home Health (HH) Blood Pressure Observations
    private Observation buildHomeHealthBloodPressureObservation(String patientId, Encounter enc,
                                                                FhirConfigManager fcm) {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        o.setEncounter(new Reference().setReference(URN_UUID + enc.getId()));

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        o.getCode().addCoding()
                .setCode(fcm.getBpCode())           // 55284-4
                .setSystem(fcm.getBpSystem())       // http://loinc.org
                .setDisplay(fcm.getBpDisplay());

        if (StringUtils.isNotEmpty(fcm.getBpHomeSystem()) && StringUtils.isNotEmpty(fcm.getBpHomeCode())) {
            o.getCode().addCoding()
                    .setCode(fcm.getBpHomeCode())
                    .setSystem(fcm.getBpHomeSystem())
                    .setDisplay(fcm.getBpHomeDisplay());
        }

        addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(readingDate));

        Observation.ObservationComponentComponent occSystolic = new Observation.ObservationComponentComponent();
        occSystolic.getCode().addCoding()
                .setCode(fcm.getBpSystolicCode())
                .setSystem(fcm.getBpSystem())
                .setDisplay(fcm.getBpSystolicDisplay());

        occSystolic.setValue(new Quantity());
        occSystolic.getValueQuantity()
                .setCode(fcm.getBpValueCode())
                .setSystem(fcm.getBpValueSystem())
                .setUnit(fcm.getBpValueUnit())
                .setValue(systolic.getValue().intValue());
        o.getComponent().add(occSystolic);

        Observation.ObservationComponentComponent occDiastolic = new Observation.ObservationComponentComponent();
        occDiastolic.getCode().addCoding()
                .setCode(fcm.getBpDiastolicCode())
                .setSystem(fcm.getBpSystem())
                .setDisplay(fcm.getBpDiastolicDisplay());

        occDiastolic.setValue(new Quantity());
        occDiastolic.getValueQuantity()
                .setCode(fcm.getBpValueCode())
                .setSystem(fcm.getBpValueSystem())
                .setUnit(fcm.getBpValueUnit())
                .setValue(diastolic.getValue().intValue());
        o.getComponent().add(occDiastolic);

        return o;
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

        o.getCode().addCoding()
                .setCode(fcm.getPulseCode())        // 8867-4
                .setSystem(fcm.getPulseSystem())    // http://loinc.org
                .setDisplay(fcm.getPulseDisplay());

        addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(readingDate));

        o.setValue(new Quantity());
        o.getValueQuantity()
                .setCode(fcm.getPulseValueCode())
                .setSystem(fcm.getPulseValueSystem())
                .setUnit(fcm.getPulseValueUnit())
                .setValue(pulse.getValue().intValue());

        return o;
    }

    private Observation buildProtocolObservation(String patientId, Encounter enc, FhirConfigManager fcm) {
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

    private void addHomeSettingExtension(DomainResource domainResource) {
        // setting MeasurementSettingExt to indicate taken in a "home" setting
        // see https://browser.ihtsdotools.org/?perspective=full&conceptId1=264362003&edition=MAIN/SNOMEDCT-US/2021-09-01&release=&languages=en

        domainResource.addExtension(new Extension()
                .setUrl("http://hl7.org/fhir/us/vitals/StructureDefinition/MeasurementSettingExt")
                .setValue(new Coding()
                        .setCode("264362003")
                        .setSystem("http://snomed.info/sct")));
    }

    private String genTemporaryId() {
        return UUID.randomUUID().toString();
    }
}
