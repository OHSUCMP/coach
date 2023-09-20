package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public class ResourceWithBundle {
    private Resource resource;
    private Bundle bundle;

    public ResourceWithBundle(Resource resource, Bundle bundle) {
        this.resource = resource;
        this.bundle = bundle;
    }

    public Resource getResource() {
        return resource;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
