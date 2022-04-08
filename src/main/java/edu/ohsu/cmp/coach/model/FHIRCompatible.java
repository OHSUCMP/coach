package edu.ohsu.cmp.coach.model;

import edu.ohsu.cmp.coach.fhir.FhirConfigManager;
import org.hl7.fhir.r4.model.Bundle;

public interface FHIRCompatible {
    Bundle toBundle(String patientId, FhirConfigManager fcm);
}
