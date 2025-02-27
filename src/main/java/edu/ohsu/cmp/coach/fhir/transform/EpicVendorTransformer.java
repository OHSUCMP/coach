package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;

public class EpicVendorTransformer extends SpecialVendorTransformer implements VendorTransformer {

    public EpicVendorTransformer(UserWorkspace workspace) {
        super(workspace);
    }

    @Override
    // note : bpObservation may be a) full BP (systolic + diastolic); b) systolic only, or c) diastolic only
    protected Observation buildHomeHealthBloodPressureObservation(ResourceType type, Observation bpObservation, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(bpObservation.getSubject());

        if (bpObservation.hasEncounter()) {
            o.setEncounter(bpObservation.getEncounter());
        }

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        FhirUtil.addHomeSettingExtension(o);

        if (bpObservation.hasCode()) {
            CodeableConcept code = bpObservation.getCode();
            if (type == ResourceType.SYSTOLIC && FhirUtil.hasCoding(code, fcm.getBpSystolicCodings())) {
                for (Coding c : fcm.getBpSystolicCustomCodings()) {
                    if (c.hasSystem() && c.getSystem().startsWith(URN_OID_PREFIX)) { // include only urn:oid Codings in Epic-destined Observations
                        o.getCode().addCoding(c);
                    }
                }
                o.setValue(bpObservation.getValueQuantity());

            } else if (type == ResourceType.DIASTOLIC && FhirUtil.hasCoding(code, fcm.getBpDiastolicCodings())) {
                for (Coding c : fcm.getBpDiastolicCustomCodings()) {
                    if (c.hasSystem() && c.getSystem().startsWith(URN_OID_PREFIX)) { // include only urn:oid Codings in Epic-destined Observations
                        o.getCode().addCoding(c);
                    }
                }
                o.setValue(bpObservation.getValueQuantity());

            } else if (FhirUtil.hasCoding(code, fcm.getBpPanelCodings())) {
                if (bpObservation.hasComponent()) {
                    if (type == ResourceType.SYSTOLIC) {
                        Observation.ObservationComponentComponent component = getComponentHavingCoding(bpObservation, fcm.getBpSystolicCodings());
                        o.setValue(component.getValueQuantity());

                    } else if (type == ResourceType.DIASTOLIC) {
                        Observation.ObservationComponentComponent component = getComponentHavingCoding(bpObservation, fcm.getBpDiastolicCodings());
                        o.setValue(component.getValueQuantity());

                    } else {
                        throw new CaseNotHandledException("invalid type");
                    }
                }

            } else {
                throw new DataException("invalid coding");
            }
        } else {
            throw new DataException("missing coding");
        }

        o.getValueQuantity().setUnit(null);         // Epic doesn't allow units to be specified

        o.setEffective(bpObservation.getEffective());

        return o;
    }

    @Override
    protected Observation buildHomeHealthBloodPressureObservation(ResourceType type, BloodPressureModel model, String patientIdRef, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientIdRef));

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        FhirUtil.addHomeSettingExtension(o);

        if (type == ResourceType.SYSTOLIC) {
            if (model.getSystolic() != null) {
                for (Coding c : fcm.getBpSystolicCustomCodings()) {
                    if (c.hasSystem() && c.getSystem().startsWith(URN_OID_PREFIX)) { // Epic flowsheet observations may only include urn:oid Codings
                        o.getCode().addCoding(c);
                    }
                }
                o.setValue(new Quantity());
                setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

            } else {
                throw new DataException("Cannot create systolic Observation - model is missing systolic value");
            }

        } else if (type == ResourceType.DIASTOLIC) {
            if (model.getDiastolic() != null) {
                for (Coding c : fcm.getBpDiastolicCustomCodings()) {
                    if (c.hasSystem() && c.getSystem().startsWith(URN_OID_PREFIX)) { // Epic flowsheet observations may only include urn:oid Codings
                        o.getCode().addCoding(c);
                    }
                }
                o.setValue(new Quantity());
                setBPValue(o.getValueQuantity(), model.getDiastolic(), fcm);

            } else {
                throw new DataException("Cannot create diastolic Observation - model is missing diastolic value");
            }

        } else {
            throw new DataException("type must be SYSTOLIC or DIASTOLIC");
        }

        o.setEffective(new DateTimeType(model.getReadingDate()));

        return o;
    }

    @Override
    protected Observation buildPulseObservation(PulseModel model, String patientId, FhirConfigManager fcm) throws DataException {
        Observation o = new Observation();

        o.setId(genTemporaryId());

        o.setSubject(new Reference().setReference(patientId));

        // Epic doesn't use encounters for user-generated records, but if it came in with one, add it

        if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        for (Coding c : fcm.getPulseCustomCodings()) {
            if (c.hasSystem() && c.getSystem().startsWith(URN_OID_PREFIX)) { // Epic flowsheet observations may only include urn:oid Codings
                o.getCode().addCoding(c);
            }
        }

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        o.setValue(new Quantity());
        o.getValueQuantity()
                .setCode(fcm.getPulseValueCode())
                .setSystem(fcm.getPulseValueSystem())
//                .setUnit(fcm.getPulseValueUnit())     // Epic doesn't like units
                .setValue(model.getPulse().getValue().intValue());

        return o;
    }
}
