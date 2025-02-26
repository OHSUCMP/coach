package edu.ohsu.cmp.coach.fhir.transform;

import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import edu.ohsu.cmp.coach.model.BloodPressureModel;
import edu.ohsu.cmp.coach.util.FhirUtil;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;

public class OracleVendorTransformer extends NonStandardVendorTransformer implements VendorTransformer {
    public OracleVendorTransformer(UserWorkspace workspace) {
        super(workspace);
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
                    o.getCode().addCoding(c);
                }
                o.setValue(new Quantity());
                setBPValue(o.getValueQuantity(), model.getSystolic(), fcm);

            } else {
                throw new DataException("Cannot create systolic Observation - model is missing systolic value");
            }

        } else if (type == ResourceType.DIASTOLIC) {
            if (model.getDiastolic() != null) {
                for (Coding c : fcm.getBpDiastolicCustomCodings()) {
                    o.getCode().addCoding(c);
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
}
