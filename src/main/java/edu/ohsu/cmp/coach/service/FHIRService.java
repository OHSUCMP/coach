package edu.ohsu.cmp.coach.service;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.gclient.ITransactionTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.http.HttpRequest;
import edu.ohsu.cmp.coach.http.HttpResponse;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.fhir.jwt.AccessToken;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

@Service
public class FHIRService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${fhir.search.count}")
    private int searchCount;

    @Autowired
    private JWTService jwtService;

    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, Class<T> aClass, Reference reference) {
        logger.info("read by reference: " + reference + " (" + aClass.getName() + ")");

        if (reference == null) return null;

        if (reference.hasReference()) {
            return readByReference(fcc, aClass, reference.getReference());

        } else if (reference.hasIdentifier()) {
            return readByIdentifier(fcc, aClass, reference.getIdentifier());

        } else {
            logger.warn("Reference does not contain reference or identifier!  returning null");
        }

        return null;
    }

    public <T extends IBaseResource> T readByIdentifier(FHIRCredentialsWithClient fcc, Class<T> aClass, Identifier identifier) {
        return readByIdentifier(fcc, aClass, identifier, null);
    }

    public <T extends IBaseResource> T readByIdentifier(FHIRCredentialsWithClient fcc, Class<T> aClass, Identifier identifier, Bundle bundle) {
        logger.info("read by identifier: " + identifier + " (" + aClass.getName() + ")");

        if (bundle != null && FhirUtil.bundleContainsResourceWithIdentifier(bundle, identifier)) {
            return FhirUtil.getResourceFromBundleByIdentifier(bundle, aClass, identifier);

        } else {
            String s = FhirUtil.toIdentifierString(identifier);
            Bundle b = search(fcc, aClass.getSimpleName() + "/?identifier=" + s);

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

    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, Class<T> aClass, String reference) {
        return readByReference(fcc, aClass, reference, null);
    }

    // version of the read function that first queries the referenced Bundle for the referenced resource
    // only executes API service call if the referenced resource isn't found
    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, Class<T> aClass, String reference, Bundle bundle) {
        logger.info("read: " + reference + " (" + aClass.getName() + ")");

        if (bundle != null && FhirUtil.bundleContainsReference(bundle, reference)) {
            return FhirUtil.getResourceFromBundleByReference(bundle, aClass, reference);

        } else {
            String id = FhirUtil.extractIdFromReference(reference);
            try {
                return fcc.getClient().read()
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
    public Bundle search(FHIRCredentialsWithClient fcc, String fhirQuery) {
        return search(fcc, fhirQuery, null);
    }

    public Bundle search(FHIRCredentialsWithClient fcc, String fhirQuery, Function<Resource, Boolean> validityFunction) {
        if (fhirQuery == null || fhirQuery.trim().equals("")) return null;

        logger.info("search: executing query: " + fhirQuery);

        Bundle bundle;
        try {
            bundle = fcc.getClient().search()
                    .byUrl(fcc.getCredentials().getServerURL() + '/' + fhirQuery)
                    .count(searchCount)
                    .returnBundle(Bundle.class)
                    .execute();

            logger.info("search: got " + bundle.getTotal() + " records for query: " + fhirQuery);
            if (logger.isDebugEnabled()) {
                logger.debug("bundle = " + FhirUtil.toJson(bundle));
            }

        } catch (InvalidRequestException ire) {
            logger.error("caught " + ire.getClass().getName() + " executing search: " + fhirQuery, ire);
            throw ire;
        }

        if (validityFunction != null) {
            filterInvalidResources(bundle, validityFunction);
        }

        if (bundle.getLink(Bundle.LINK_NEXT) == null) {
            return bundle;

        } else {
            CompositeBundle compositeBundle = new CompositeBundle();
            compositeBundle.consume(bundle);

            int page = 2;
            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = fcc.getClient().loadPage().next(bundle).execute();

                logger.info("search (page " + page + "): " + fhirQuery + " (size=" + bundle.getTotal() + ")");
                if (logger.isDebugEnabled()) {
                    logger.debug("bundle = " + FhirUtil.toJson(bundle));
                }

                if (validityFunction != null) {
                    filterInvalidResources(bundle, validityFunction);
                }

                compositeBundle.consume(bundle);

                page ++;
            }

            return compositeBundle.getBundle();
        }
    }

    public Bundle transact(FHIRCredentialsWithClient fcc, Bundle bundle) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("transacting Bundle: " + FhirUtil.toJson(bundle));
        }

        ITransactionTyped<Bundle> itt = fcc.getClient().transaction().withBundle(bundle)
                .withAdditionalHeader("Prefer", "return=representation");

        if (fcc.getCredentials().hasJwt()) {
            // see: https://apporchard.epic.com/Article?docId=oauth2&section=BackendOAuth2Guide
            // see: https://jwt.io/

            CapabilityStatement metadata = getMetadata(fcc);
            logger.info("got metadata: " + FhirUtil.toJson(metadata));

            AccessToken accessToken = jwtService.getAccessToken(fcc.getCredentials().getServerURL(), fcc.getCredentials().getJwt());
            itt = itt.withAdditionalHeader("Authorization", "Bearer " + accessToken.getAccessToken());
        }

        Bundle response = itt.execute();

        if (logger.isDebugEnabled()) {
            logger.debug("transaction response: " + FhirUtil.toJson(response));
        }

        return response;
    }

    public CapabilityStatement getMetadata(FHIRCredentialsWithClient fcc) {
        String url = fcc.getCredentials().getServerURL() + "/metadata";
        try {
            HttpResponse response = new HttpRequest().get(url);
            IParser parser = fcc.getClient().getFhirContext().newJsonParser();
            return parser.parseResource(CapabilityStatement.class, response.getResponseBody());

        } catch (Exception e) {
            logger.error("caught " + e.getClass().getName() + " getting metadata from " + url + " - " + e.getMessage(), e);
            return null;
        }
    }


//////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private void filterInvalidResources(Bundle bundle, Function<Resource, Boolean> validityFunction) {
        if (bundle != null && bundle.hasEntry()) {
            Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                if (entry.hasResource()) {
                    boolean isValid = validityFunction.apply(entry.getResource());
                    if ( ! isValid ) {
                        iter.remove();
                    }
                }
            }
        }
    }
}
