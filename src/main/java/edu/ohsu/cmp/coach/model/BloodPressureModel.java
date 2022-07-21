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

public class BloodPressureModel extends AbstractVitalsModel implements FHIRCompatible {
    private Observation sourceBPObservation;

    private QuantityModel systolic;
    private QuantityModel diastolic;

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
    public BloodPressureModel(Source source, Integer systolic, Integer diastolic,
                              Date readingDate, Boolean followedProtocol,
                              FhirConfigManager fcm) {

        super(source, followedProtocol, readingDate);

        this.systolic = new QuantityModel(systolic, fcm.getBpValueUnit());
        this.diastolic = new QuantityModel(diastolic, fcm.getBpValueUnit());
        this.readingDate = readingDate; //.getTime();
    }

    // read local
    public BloodPressureModel(HomeBloodPressureReading reading, FhirConfigManager fcm) {
        super(Source.HOME, reading.getFollowedInstructions(), reading.getReadingDate());

        systolic = new QuantityModel(reading.getSystolic(), fcm.getBpValueUnit());
        diastolic = new QuantityModel(reading.getDiastolic(), fcm.getBpValueUnit());
        readingDate = reading.getReadingDate(); //.getTime();
    }

    // read remote
    public BloodPressureModel(Encounter enc, Observation bpObservation,
                              Observation protocolObservation,
                              FhirConfigManager fcm) throws DataException {

        super(enc, bpObservation, protocolObservation, fcm);

        this.sourceBPObservation = bpObservation;

        // todo : set id.  but to what?  first Observation's id?  what about the others?  how is id used?  do we need
        //        to retain the ids for the Encounter and other Observations?

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
    }

    @Override
    public String getReadingType() {
        return "BP";
    }

    @Override
    public String getValue() {
        return systolic.getValue() + "/" + diastolic.getValue() + " " + systolic.getUnit();
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

    @Override
    public Bundle toBundle(String patientId, FhirConfigManager fcm) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        if (sourceEncounter != null && sourceBPObservation != null) {
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourceEncounter));
            bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(sourceBPObservation));
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

            if (followedProtocol != null && followedProtocol) {
                Observation protocolObservation = buildProtocolObservation(patientIdRef, enc, fcm);
                bundle.getEntry().add(new Bundle.BundleEntryComponent().setResource(protocolObservation));
            }
        }

        return bundle;
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
}
