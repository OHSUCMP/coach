package edu.ohsu.cmp.htnu18app.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.repository.PatientRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    @Autowired
    private PatientRepository repository;

    public Patient getPatient(IGenericClient client, String id) {
        Patient p = client
                .read()
                .resource(Patient.class)
                .withId(id)
                .execute();

        String hash = DigestUtils.sha256Hex(p.getId());
        if (! repository.existsPatientByPatIdHash(hash) ) {
            edu.ohsu.cmp.htnu18app.entity.Patient p2 = new edu.ohsu.cmp.htnu18app.entity.Patient(hash);
            repository.save(p2);
        }

        return p;
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
