package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.entity.app.MyPatient;
import edu.ohsu.cmp.htnu18app.repository.app.PatientRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PatientService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private PatientRepository repository;

    public Long getInternalPatientId(String fhirPatientId) {
        String patIdHash = buildPatIdHash(fhirPatientId);

        MyPatient p;
        if (repository.existsPatientByPatIdHash(patIdHash)) {
            p = repository.findOneByPatIdHash(patIdHash);

        } else {
            p = new MyPatient(patIdHash);
            p = repository.save(p);
        }

        return p.getId();
    }

    private String buildPatIdHash(String patientId) {
        return DigestUtils.sha256Hex(patientId + salt);
    }
}
