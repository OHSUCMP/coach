package edu.ohsu.cmp.coach.model;

import org.hl7.fhir.r4.model.Bundle;

public interface FHIRCompatible {
    Bundle toBundle();
}
