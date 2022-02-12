package edu.ohsu.cmp.coach.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FHIRCredentialsWithClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private FHIRCredentials credentials;
    private IGenericClient client;

    public FHIRCredentialsWithClient(FHIRCredentials credentials, IGenericClient client) {
        this.credentials = credentials;
        this.client = client;
    }

    public FHIRCredentials getCredentials() {
        return credentials;
    }

    public IGenericClient getClient() {
        return client;
    }

    public <T extends IBaseResource> T read(Class<T> aClass, String reference) {
        return read(aClass, reference, null);
    }

    // version of the read function that first queries the referenced Bundle for the referenced resource
    // only executes API service call if the referenced resource isn't found
    public <T extends IBaseResource> T read(Class<T> aClass, String reference, Bundle bundle) {
        logger.info("read: " + reference + " (" + aClass.getName() + ")");

        T t;
        if (bundle != null && FhirUtil.bundleContainsReference(bundle, reference)) {
            t = FhirUtil.getResourceFromBundleByReference(bundle, aClass, reference);

        } else {
            String id = FhirUtil.extractIdFromReference(reference);
            try {
                t = client.read()
                        .resource(aClass)
                        .withId(id)
                        .execute();
            } catch (InvalidRequestException ire) {
                logger.error("caught " + ire.getClass().getName() + " reading " + aClass.getName() + " with id='" + id + "' - " + ire.getMessage());
                throw ire;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("resource = " + FhirUtil.toJson(t));
        }

        return t;
    }

    // search function to facilitate getting large datasets involving multi-paginated queries
    public Bundle search(String fhirQuery) {
        return search(fhirQuery, null);
    }

    public Bundle search(String fhirQuery, Integer limit) {
        if (fhirQuery == null || fhirQuery.trim().equals("")) return null;

        Bundle bundle;
        try {
            bundle = client.search()
                    .byUrl(credentials.getServerURL() + '/' + fhirQuery)
                    .returnBundle(Bundle.class)
                    .execute();

            logger.info("search: " + fhirQuery + " (size=" + bundle.getTotal() + ")");

        } catch (InvalidRequestException ire) {
            logger.error("caught " + ire.getClass().getName() + " executing search: " + fhirQuery, ire);
            throw ire;
        }

        if (bundle.getLink(Bundle.LINK_NEXT) == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("bundle = " + FhirUtil.toJson(bundle));
            }

            return FhirUtil.truncate(bundle, limit);

        } else {
            Bundle compositeBundle = new Bundle();
            compositeBundle.setType(Bundle.BundleType.COLLECTION);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                compositeBundle.getEntry().add(entry);
            }

            int page = 2;
            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                if (limit != null && compositeBundle.getEntry().size() >= limit) {
                    break;
                }

                bundle = client.loadPage().next(bundle).execute();

                logger.info("search (page " + page + "): " + fhirQuery + " (size=" + bundle.getTotal() + ")");

                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    compositeBundle.getEntry().add(entry);
                }

                page ++;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("compositeBundle = " + FhirUtil.toJson(compositeBundle));
            }

            return FhirUtil.truncate(compositeBundle, limit);
        }
    }

}
