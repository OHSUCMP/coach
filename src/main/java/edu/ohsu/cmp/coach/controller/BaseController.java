package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseController {
    @Value("${application.name}")
    protected String applicationName;

    @Autowired
    protected FhirConfigManager fcm;
}
