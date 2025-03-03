package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.model.PulseModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;

public class OracleVendorTransformer extends SpecialVendorTransformer implements VendorTransformer {
    private static final String FHIR_CERNER_COM_PREFIX = "https://fhir.cerner.com/";
    private static final String PULSE_VALUE_CODE = "{Beats}/min";

    public OracleVendorTransformer(UserWorkspace workspace) {
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
                for (Coding c : fcm.getBpSystolicCodings()) {
                    // Oracle Observations may only include https://fhir.cerner.com/ Codings
                    if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                        o.getCode().addCoding(c);
                        break;  // Oracle requires one and only one coding to be added
                    }
                }

                o.setValue(bpObservation.getValueQuantity());

            } else if (type == ResourceType.DIASTOLIC && FhirUtil.hasCoding(code, fcm.getBpDiastolicCodings())) {
                for (Coding c : fcm.getBpDiastolicCodings()) {
                    // Oracle Observations may only include https://fhir.cerner.com/ Codings
                    if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                        o.getCode().addCoding(c);
                        break;  // Oracle requires one and only one coding to be added
                    }
                }

                o.setValue(bpObservation.getValueQuantity());

            } else if (FhirUtil.hasCoding(code, fcm.getBpPanelCodings())) {
                if (bpObservation.hasComponent()) {
                    if (type == ResourceType.SYSTOLIC) {
                        for (Coding c : fcm.getBpSystolicCodings()) {
                            // Oracle Observations may only include https://fhir.cerner.com/ Codings
                            if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                                o.getCode().addCoding(c);
                                break;  // Oracle requires one and only one coding to be added
                            }
                        }

                        Observation.ObservationComponentComponent component = getComponentHavingCoding(bpObservation, fcm.getBpSystolicCodings());
                        o.setValue(component.getValueQuantity());

                    } else if (type == ResourceType.DIASTOLIC) {
                        for (Coding c : fcm.getBpDiastolicCodings()) {
                            // Oracle Observations may only include https://fhir.cerner.com/ Codings
                            if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                                o.getCode().addCoding(c);
                                break;  // Oracle requires one and only one coding to be added
                            }
                        }

                        Observation.ObservationComponentComponent component = getComponentHavingCoding(bpObservation, fcm.getBpDiastolicCodings());
                        o.setValue(component.getValueQuantity());

                    } else {
                        throw new CaseNotHandledException("cannot handle case where type=" + type);
                    }

                } else {
                    throw new DataException("missing component");
                }

            } else {
                throw new DataException("invalid coding");
            }
        } else {
            throw new DataException("missing coding");
        }

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
                for (Coding c : fcm.getBpSystolicCodings()) {
                    // Oracle Observations may only include https://fhir.cerner.com/ Codings
                    if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                        o.getCode().addCoding(c);
                        break;  // Oracle requires one and only one coding to be added
                    }
                }

                o.setValue(new Quantity());
                setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

            } else {
                throw new DataException("Cannot create systolic Observation - model is missing systolic value");
            }

        } else if (type == ResourceType.DIASTOLIC) {
            if (model.getDiastolic() != null) {
                for (Coding c : fcm.getBpDiastolicCodings()) {
                    // Oracle Observations may only include https://fhir.cerner.com/ Codings
                    if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                        o.getCode().addCoding(c);
                        break;  // Oracle requires one and only one coding to be added
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

        // Oracle doesn't use Encounters for user-generated records, but if it came in with one, add it
        if (model.getSourceEncounter() != null) {
            o.setEncounter(new Reference().setReference(FhirUtil.toRelativeReference(model.getSourceEncounter())));
        }

        o.setStatus(Observation.ObservationStatus.FINAL);

        o.addCategory().addCoding()
                .setCode(OBSERVATION_CATEGORY_CODE)
                .setSystem(OBSERVATION_CATEGORY_SYSTEM)
                .setDisplay("vital-signs");

        for (Coding c : fcm.getPulseCodings()) {
            // Oracle Observations may only include https://fhir.cerner.com/ Codings
            if (c.hasSystem() && c.getSystem().startsWith(FHIR_CERNER_COM_PREFIX)) {
                o.getCode().addCoding(c);
                break;  // Oracle requires one and only one coding to be added
            }
        }

        FhirUtil.addHomeSettingExtension(o);

        o.setEffective(new DateTimeType(model.getReadingDate()));

        o.setValue(new Quantity());
        o.getValueQuantity()
                .setCode(PULSE_VALUE_CODE)            // Oracle requires that a special code be put here
                .setSystem(fcm.getPulseValueSystem())
                .setUnit(fcm.getPulseValueUnit())
                .setValue(model.getPulse().getValue().intValue());

        return o;
    }
}
