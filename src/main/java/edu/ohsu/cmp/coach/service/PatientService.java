package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.repository.PatientRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PatientService extends AbstractService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${security.salt}")
    private String salt;

    @Autowired
    private PatientRepository repository;

    @Autowired
    private EHRService ehrService;

    public PatientModel buildPatient(String sessionId) {
        return new PatientModel(ehrService.getPatient(sessionId));
    }

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


///////////////////////////////////////////////////////////////////////
// private methods
//

    private String buildPatIdHash(String patientId) {
        return DigestUtils.sha256Hex(patientId + salt);
    }
}
