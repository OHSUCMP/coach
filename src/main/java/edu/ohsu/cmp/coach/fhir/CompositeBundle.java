package edu.ohsu.cmp.coach.fhir;

import edu.ohsu.cmp.coach.exception.CaseNotHandledException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public class CompositeBundle {
    private Bundle bundle;

    public CompositeBundle() {
        bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
    }

    public void consume(IBaseResource resource) {
        if (resource != null) {
            if (resource instanceof Bundle) {
                for (Bundle.BundleEntryComponent entry : ((Bundle) resource).getEntry()) {
                    bundle.addEntry(entry.copy());
                }

            } else if (resource instanceof Resource) {
                Resource r = (Resource) resource;
                bundle.addEntry(new Bundle.BundleEntryComponent().setFullUrl(r.getId()).setResource(r));

            } else {
                throw new CaseNotHandledException("couldn't handle " + resource.getClass().getName());
            }
        }
    }

    public Bundle getBundle() {
        return bundle;
    }
}
