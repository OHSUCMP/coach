package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.entity.ClinicContact;
import edu.ohsu.cmp.coach.repository.ClinicContactRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClinicContactService {

    @Autowired
    private ClinicContactRepository clinicContactRepository;

    public List<ClinicContact> getClinicContactList() {
        return clinicContactRepository.findAllActive();
    }
}
