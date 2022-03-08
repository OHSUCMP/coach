package edu.ohsu.cmp.coach.model.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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

    public <T extends IBaseResource> T read(Class<T> aClass, Reference reference) {
        logger.info("read by reference: " + reference + " (" + aClass.getName() + ")");

        if (reference == null) return null;

        if (reference.hasReference()) {
            return read(aClass, reference.getReference());

        } else if (reference.hasIdentifier()) {
            return read(aClass, reference.getIdentifier());

        } else {
            logger.warn("Reference does not contain reference or identifier!  returning null");
        }

        return null;
    }

    public <T extends IBaseResource> T read(Class<T> aClass, Identifier identifier) {
        logger.info("read by identifier: " + identifier + " (" + aClass.getName() + ")");

        String s = toIdentifierString(identifier);
        Bundle b = search("?identifier=" + s);
        if (b.getEntry().size() == 0) {
            logger.warn("couldn't find resource with identifier=" + s);
            return null;

        } else if (b.getEntry().size() == 1) {
            Resource r = b.getEntryFirstRep().getResource();
            logger.debug("found " + r.getClass().getName() + " with identifier=" + s);
            return aClass.cast(r);

        } else {
            Resource r = b.getEntryFirstRep().getResource();
            logger.warn("found " + b.getEntry().size() + " resources associated with identifier=" + s +
                    "!  returning first match (" + r.getClass().getName() + ") -");
            return aClass.cast(r);
        }
    }

    private String toIdentifierString(Identifier identifier) {
        if (identifier == null) return null;

        List<String> parts = new ArrayList<>();
        if (identifier.hasSystem()) parts.add(identifier.getSystem());
        if (identifier.hasValue()) parts.add(identifier.getValue());

        return StringUtils.join(parts, "|");
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
