package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.fhir.FhirConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseController {
    @Value("${application.name}")
    protected String applicationName;

    @Autowired
    protected FhirConfigManager fcm;
}
