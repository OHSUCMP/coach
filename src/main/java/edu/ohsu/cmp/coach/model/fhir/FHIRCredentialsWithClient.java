package edu.ohsu.cmp.coach.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FHIRCredentialsWithClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int QUERY_COUNT = 500; // bigger is better?  bigger => fewer queries to execute

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

    public <T extends IBaseResource> T readByReference(Class<T> aClass, Reference reference) {
        logger.info("read by reference: " + reference + " (" + aClass.getName() + ")");

        if (reference == null) return null;

        if (reference.hasReference()) {
            return readByReference(aClass, reference.getReference());

        } else if (reference.hasIdentifier()) {
            return readByIdentifier(aClass, reference.getIdentifier());

        } else {
            logger.warn("Reference does not contain reference or identifier!  returning null");
        }

        return null;
    }

    public <T extends IBaseResource> T readByIdentifier(Class<T> aClass, Identifier identifier) {
        return readByIdentifier(aClass, identifier, null);
    }

    public <T extends IBaseResource> T readByIdentifier(Class<T> aClass, Identifier identifier, Bundle bundle) {
        logger.info("read by identifier: " + identifier + " (" + aClass.getName() + ")");

        if (bundle != null && FhirUtil.bundleContainsResourceWithIdentifier(bundle, identifier)) {
            return FhirUtil.getResourceFromBundleByIdentifier(bundle, aClass, identifier);

        } else {
            String s = FhirUtil.toIdentifierString(identifier);
            Bundle b = search(aClass.getSimpleName() + "/?identifier=" + s);

            if (b.getEntry().size() == 0) {
                logger.warn("couldn't find resource with identifier=" + s);
                return null;
            }

            Resource r = null;

            try {
                r = b.getEntryFirstRep().getResource();

                if (b.getEntry().size() == 1) {
                    logger.debug("found " + r.getClass().getName() + " with identifier=" + s);

                } else {
                    logger.warn("found " + b.getEntry().size() + " resources associated with identifier=" + s +
                            "!  returning first match (" + r.getClass().getName() + ") -");
                }

                return aClass.cast(r);

            } catch (ClassCastException cce) {
                logger.error("caught " + cce.getClass().getName() + " attempting to cast " + r.getClass().getName() + " to " + aClass.getName());
                logger.debug(r.getClass().getName() + " : " + FhirUtil.toJson(r));
                throw cce;
            }
        }
    }

    public <T extends IBaseResource> T readByReference(Class<T> aClass, String reference) {
        return readByReference(aClass, reference, null);
    }

    // version of the read function that first queries the referenced Bundle for the referenced resource
    // only executes API service call if the referenced resource isn't found
    public <T extends IBaseResource> T readByReference(Class<T> aClass, String reference, Bundle bundle) {
        logger.info("read: " + reference + " (" + aClass.getName() + ")");

        if (bundle != null && FhirUtil.bundleContainsReference(bundle, reference)) {
            return FhirUtil.getResourceFromBundleByReference(bundle, aClass, reference);

        } else {
            String id = FhirUtil.extractIdFromReference(reference);
            try {
                return client.read()
                        .resource(aClass)
                        .withId(id)
                        .execute();
            } catch (InvalidRequestException ire) {
                logger.error("caught " + ire.getClass().getName() + " reading " + aClass.getName() + " with id='" + id + "' - " + ire.getMessage());
                throw ire;
            }
        }
    }

    // search function to facilitate getting large datasets involving multi-paginated queries
    public Bundle search(String fhirQuery) {
        return search(fhirQuery, null);
    }

    public Bundle search(String fhirQuery, Integer limit) {
        if (fhirQuery == null || fhirQuery.trim().equals("")) return null;

        logger.info("search: " + fhirQuery);

        Bundle bundle;
        try {
            bundle = client.search()
                    .byUrl(credentials.getServerURL() + '/' + fhirQuery)
                    .count(QUERY_COUNT)
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

    public Bundle transact(Bundle bundle) {
        if (logger.isDebugEnabled()) {
            logger.debug("transacting Bundle: " + FhirUtil.toJson(bundle));
        }

        Bundle response = client.transaction().withBundle(bundle)
                .withAdditionalHeader("Prefer", "return=representation")
                .execute();

        if (logger.isDebugEnabled()) {
            logger.debug("transaction response: " + FhirUtil.toJson(response));
        }

        return response;
    }
}
