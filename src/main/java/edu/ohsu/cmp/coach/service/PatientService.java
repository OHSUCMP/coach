package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.model.PatientModel;
import edu.ohsu.cmp.coach.repository.PatientRepository;
import edu.ohsu.cmp.coach.util.UUIDUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

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

    public MyPatient getMyPatient(String fhirPatientId) {
        String patIdHash = buildPatIdHash(fhirPatientId);

        MyPatient p;
        if (repository.existsPatientByPatIdHash(patIdHash)) {
            p = repository.findOneByPatIdHash(patIdHash);
            if (StringUtils.isBlank(p.getRedcapId())) {
                p.setRedcapId(UUIDUtil.getRandomUUID());
                p = repository.save(p);
            }

        } else {
            p = new MyPatient(patIdHash);
            p.setRedcapId(UUIDUtil.getRandomUUID());
            p.setBpGoalUpdated(false);
            p = repository.save(p);
        }

        return p;
    }

    public void setOmronLastUpdated(Long internalPatientId, Date omronLastUpdated) {
        Optional<MyPatient> p = repository.findById(internalPatientId);
        if (p.isPresent()) {
            MyPatient myPatient = p.get();
            myPatient.setOmronLastUpdated(omronLastUpdated);
            repository.save(myPatient);
        }
    }

    public void setBPGoalUpdated(Long internalPatientId, Boolean bpGoalUpdated) {
        Optional<MyPatient> p = repository.findById(internalPatientId);
        if (p.isPresent()) {
            MyPatient myPatient = p.get();
            myPatient.setBpGoalUpdated(bpGoalUpdated);
            repository.save(myPatient);
        }
    }

    public MyPatient update(MyPatient myPatient) {
        return repository.save(myPatient);
    }

///////////////////////////////////////////////////////////////////////
// private methods
//

    private String buildPatIdHash(String patientId) {
        return DigestUtils.sha256Hex(patientId + salt);
    }
}
