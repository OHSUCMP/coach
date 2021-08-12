package edu.ohsu.cmp.htnu18app.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
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

    public <T extends IBaseResource> T read(Class<T> aClass, String id) {
        return client.read()
                .resource(aClass)
                .withId(id)
                .execute();
    }

    // search function to facilitate getting large datasets involving multi-paginated queries
    public Bundle search(String fhirQuery) {
        if (fhirQuery == null || fhirQuery.trim().equals("")) return null;

        Bundle bundle = client.search()
                .byUrl(credentials.getServerURL() + '/' + fhirQuery)
                .returnBundle(Bundle.class)
                .execute();

        logger.info("search: " + fhirQuery + " (size=" + bundle.getTotal() + ")");

        if (bundle.getLink(Bundle.LINK_NEXT) == null) {
            return bundle;

        } else {
            Bundle compositeBundle = new Bundle();
            compositeBundle.setType(Bundle.BundleType.COLLECTION);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                compositeBundle.getEntry().add(entry);
            }

            int page = 2;
            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = client.loadPage().next(bundle).execute();

                logger.info("search (page " + page + "): " + fhirQuery + " (size=" + bundle.getTotal() + ")");

                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    compositeBundle.getEntry().add(entry);
                }

                page ++;
            }

            return compositeBundle;
        }
    }
}
