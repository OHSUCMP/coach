package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.repository.PatientRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PatientService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PatientRepository repository;

    public Patient getPatient(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Patient p = cache.getPatient();
        if (p == null) {
            logger.info("requesting Patient data for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            p = fcc.getClient()
                    .read()
                    .resource(Patient.class)
                    .withId(fcc.getCredentials().getPatientId())
                    .execute();
            cache.setPatient(p);
        }
        return p;
    }

    public Long getInternalPatientId(String fhirPatientId) {
        String patIdHash = buildPatIdHash(fhirPatientId);

        edu.ohsu.cmp.htnu18app.entity.Patient p;
        if (repository.existsPatientByPatIdHash(patIdHash)) {
            p = repository.findOneByPatIdHash(patIdHash);

        } else {
            p = new edu.ohsu.cmp.htnu18app.entity.Patient(patIdHash);
            p = repository.save(p);
        }

        return p.getId();
    }

    private String buildPatIdHash(String patientId) {
        return DigestUtils.sha256Hex(patientId);
    }

    public Bundle getBloodPressureObservations(String sessionId) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Bundle b = cache.getBpList();
        if (b == null) {
            logger.info("requesting Blood Pressure data for session " + sessionId);

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();
            b = fcc.getClient()
                    .search()
                    .forResource(Observation.class)
                    .and(Observation.PATIENT.hasId(fcc.getCredentials().getPatientId()))
                    .and(Observation.CODE.exactly().systemAndCode(BloodPressureModel.SYSTEM, BloodPressureModel.CODE))
                    .returnBundle(Bundle.class)
                    .execute();
            cache.setBpList(b);
        }
        return b;
    }
}
