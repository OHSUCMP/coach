package edu.ohsu.cmp.htnu18app.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
    public Patient getPatient(IGenericClient client, String id) {
        return client
                .read()
                .resource(Patient.class)
                .withId(id)
                .execute();
    }

    public Bundle getBloodPressureObservations(IGenericClient client, String patientId) {
        return client
                .search()
                .forResource((Observation.class))
                .and(Observation.PATIENT.hasId(patientId))
                .and(Observation.CODE.exactly().systemAndCode(BloodPressureModel.SYSTEM, BloodPressureModel.CODE))
                .returnBundle(Bundle.class)
                .execute();
    }


}
