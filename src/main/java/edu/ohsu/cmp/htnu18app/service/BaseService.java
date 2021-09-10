package edu.ohsu.cmp.htnu18app.service;

import edu.ohsu.cmp.htnu18app.fhir.FhirConfigManager;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseService {
    @Autowired
    protected FhirConfigManager fcm;
}
