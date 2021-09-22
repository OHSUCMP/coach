package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseService {
    @Autowired
    protected FhirConfigManager fcm;
}
