package edu.ohsu.cmp.coach.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.*;
import edu.ohsu.cmp.coach.exception.*;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.FhirStrategy;
import edu.ohsu.cmp.coach.model.ResourceWithBundle;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.fhir.jwt.AccessToken;
import edu.ohsu.cmp.coach.util.FhirUtil;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class FHIRService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${fhir.search.count}")
    private int searchCount;

    @Value("${fhir.retry.count:5}")
    private Integer maxRetries;

    @Value("${smart.backend.iss}")
    private String backendIss;

    @Autowired
    private AccessTokenService accessTokenService;

    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, FhirStrategy strategy, Class<T> aClass,
                                                       Reference reference) throws DataException, ConfigurationException, IOException {
        if (reference == null) return null;

        T t;
        if (reference.hasReference()) {
            t = readByReference(fcc, strategy, aClass, reference.getReference());
            if (t != null) return t;
        }

        if (reference.hasIdentifier()) {
            t = readByIdentifier(fcc, strategy, aClass, reference.getIdentifier());
            if (t != null) return t;
        }

        logger.warn("Reference does not contain reference or identifier!  returning null");

        return null;
    }

    public <T extends IBaseResource> T readByIdentifier(FHIRCredentialsWithClient fcc, FhirStrategy strategy, Class<T> aClass,
                                                        Identifier identifier) throws DataException, ConfigurationException, IOException {
        return readByIdentifier(fcc, strategy, aClass, identifier, null);
    }

    public <T extends IBaseResource> T readByIdentifier(FHIRCredentialsWithClient fcc, FhirStrategy strategy, Class<T> aClass,
                                                        Identifier identifier, Map<String, String> headers) throws DataException, ConfigurationException, IOException {
        String identifierString = FhirUtil.toIdentifierString(identifier);
        Bundle b = search(fcc, strategy, aClass.getSimpleName() + "/?identifier=" + identifierString, headers);

        if (b.getEntry().isEmpty()) {
            logger.warn("couldn't find resource with identifier={}", identifierString);
            return null;
        }

        Resource r = null;

        try {
            r = b.getEntryFirstRep().getResource();

            if (b.getEntry().size() == 1) {
                logger.debug("found {} with identifier={}", r.getClass().getName(), identifierString);

            } else {
                logger.warn("found {} resources associated with identifier={}!  returning first match ({}) -",
                        b.getEntry().size(), identifierString, r.getClass().getName());
            }

            return aClass.cast(r);

        } catch (ClassCastException cce) {
            String classNameForLogging = r != null ?
                    r.getClass().getSimpleName() :
                    "null";

            logger.error("caught {} attempting to cast {} to {}", cce.getClass().getName(), classNameForLogging, aClass.getName());
            if (logger.isDebugEnabled()) {
                logger.debug("{} : {}", classNameForLogging, FhirUtil.toJsonForLogging(r));
            }
            throw cce;
        }
    }

    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, FhirStrategy strategy, @NotNull Class<T> aClass,
                                                       String reference) throws DataException, ConfigurationException, IOException {
        return readByReference(fcc, strategy, aClass, reference, null);
    }

    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, FhirStrategy strategy, @NotNull Class<T> aClass,
                                                       String reference, Map<String, String> headers) throws DataException, ConfigurationException, IOException {
        return readByReference(buildClient(fcc, strategy), aClass, reference, headers);
    }

    public <T extends IBaseResource> T readByReference(IGenericClient client, @NotNull Class<T> aClass, String reference) throws DataException, ConfigurationException, IOException {
        return readByReference(client, aClass, reference, null);
    }

    public <T extends IBaseResource> T readByReference(IGenericClient client, @NotNull Class<T> aClass, String reference, Map<String, String> headers) throws DataException, ConfigurationException, IOException {
        logger.info("read: {}", reference);
        String id = FhirUtil.extractIdFromReference(reference);

        int attempt = 0;
        while (attempt++ < maxRetries) {
            try {
                IReadExecutable<T> read = client.read()
                        .resource(aClass)
                        .withId(id);

                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        read = read.withAdditionalHeader(entry.getKey(), entry.getValue());
                    }
                }

                return read.execute();

            } catch (InvalidRequestException ire) {
                logger.error("caught {} reading {} - {}", ire.getClass().getName(), reference, ire.getMessage());
                throw ire;

            } catch (InternalErrorException iee) {
                // HTTP 500 Internal Server Error - retry
                if (canRetry(attempt)) {
                    logger.debug("caught {} while reading: {} - retrying -", iee.getClass().getSimpleName(), reference);
                } else {
                    throw iee;
                }

            } catch (UnclassifiedServerFailureException usfe) {
                if (usfe.getStatusCode() == 504 && canRetry(attempt)) { // gateway timeout - retry
                    logger.debug("caught HTTP 504 Bad Gateway while reading {} - retrying -", reference);
                } else {
                    throw usfe;
                }
            }
        }
        throw new DataException("failed to read " + reference + " after " + maxRetries + " attempts");
    }

    // search function to facilitate getting large datasets involving multi-paginated queries
    public Bundle search(FHIRCredentialsWithClient fcc, FhirStrategy strategy, String fhirQuery) throws DataException, ConfigurationException, IOException {
        return search(fcc, strategy, fhirQuery, null);
    }

    public Bundle search(FHIRCredentialsWithClient fcc, FhirStrategy strategy, String fhirQuery, Map<String, String> headers) throws DataException, ConfigurationException, IOException {
        return search(fcc, strategy, fhirQuery, headers, null, null);
    }

    public Bundle search(FHIRCredentialsWithClient fcc, FhirStrategy strategy, String fhirQuery,
                         Function<ResourceWithBundle, Boolean> validityFunction,
                         Function<ResourceWithBundle, List<Resource>> supplementalResourcesFunction) throws DataException, ConfigurationException, IOException {
        return search(fcc, strategy, fhirQuery, null, validityFunction, supplementalResourcesFunction);
    }

    public Bundle search(FHIRCredentialsWithClient fcc, FhirStrategy strategy, String fhirQuery, Map<String, String> headers,
                         Function<ResourceWithBundle, Boolean> validityFunction,
                         Function<ResourceWithBundle, List<Resource>> supplementalResourcesFunction) throws DataException, ConfigurationException, IOException {
        if (strategy == FhirStrategy.DISABLED) {
            return null;
        }

        IGenericClient client = buildClient(fcc, strategy);

        return search(client, fcc.getCredentials().getServerURL(), fhirQuery, headers, validityFunction, supplementalResourcesFunction);
    }

    public Bundle search(IGenericClient client, String fhirServerURL, String fhirQuery) throws DataException, ConfigurationException, IOException {
        return search(client, fhirServerURL, fhirQuery, null);
    }

    public Bundle search(IGenericClient client, String fhirServerURL, String fhirQuery, Map<String, String> headers) throws DataException, ConfigurationException, IOException {
        return search(client, fhirServerURL, fhirQuery, headers, null, null);
    }

    public Bundle search(IGenericClient client, String fhirServerURL, String fhirQuery,
                         Function<ResourceWithBundle, Boolean> validityFunction,
                         Function<ResourceWithBundle, List<Resource>> supplementalResourcesFunction) throws DataException, ConfigurationException, IOException {
        return search(client, fhirServerURL, fhirQuery, null, validityFunction, supplementalResourcesFunction);
    }

    public Bundle search(IGenericClient client, String fhirServerURL, String fhirQuery, Map<String, String> headers,
                         Function<ResourceWithBundle, Boolean> validityFunction,
                         Function<ResourceWithBundle, List<Resource>> supplementalResourcesFunction) throws DataException, ConfigurationException, IOException {

        if (StringUtils.isBlank(fhirQuery)) {
            return null;
        }

        logger.info("search: executing query: {}", fhirQuery);

        Bundle bundle = null;
        int attempt = 0;
        while (attempt++ < maxRetries) {
            try {
                IQuery<Bundle> query = client.search()
                        .byUrl(fhirServerURL + '/' + fhirQuery)
                        .count(searchCount)
                        .accept("application/fhir+json")        // required for Cerner
                        .returnBundle(Bundle.class);

                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        query = query.withAdditionalHeader(entry.getKey(), entry.getValue());
                    }
                }

                bundle = query.execute();

                // bundle.getTotal() may be null and if so it will return 0, even if there are many entries.  Cerner does this
                logger.info("search: got Bundle with total={}, entries={} for query: {}", bundle.getTotal(), bundle.getEntry().size(), fhirQuery);
                if (logger.isDebugEnabled()) {
                    logger.debug("bundle = {}", FhirUtil.toJsonForLogging(bundle));
                }
                break;

            } catch (InvalidRequestException ire) {
                logger.error("caught {} executing search: {}", ire.getClass().getName(), fhirQuery, ire);
                throw ire;

            } catch (InternalErrorException iee) {
                // HTTP 500 Internal Server Error - retry
                if (canRetry(attempt)) {
                    logger.debug("caught {} executing search: {} - retrying", iee.getClass().getSimpleName(), fhirQuery);
                } else {
                    throw iee;
                }

            } catch (UnclassifiedServerFailureException usfe) {
                if (usfe.getStatusCode() == 504 && canRetry(attempt)) { // gateway timeout - retry
                    logger.debug("caught HTTP 504 Bad Gateway executing search: {} - retrying -", fhirQuery);
                } else {
                    throw usfe;
                }
            }
        }

        if (bundle != null && bundle.getLink(Bundle.LINK_NEXT) != null) {
            CompositeBundle compositeBundle = new CompositeBundle();
            compositeBundle.consume(bundle);

            int page = 2;
            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    // stop requesting pages - the task this is running in is being shut down.
                    // isInterrupted() doesn't clear the flag, so callers still see the interrupt
                    throw new DataException("interrupted paginating search: " + fhirQuery);
                }

                attempt = 0;
                while (attempt++ < maxRetries) {
                    try {
                        bundle = client.loadPage().next(bundle).execute();
                        break;

                    } catch (UnclassifiedServerFailureException usfe) {
                        if (usfe.getStatusCode() == 504 && canRetry(attempt)) { // gateway timeout - retry
                            logger.debug("caught HTTP 504 Bad Gateway getting page {} for search: {} - retrying -", page, fhirQuery);
                        } else {
                            throw usfe;
                        }
                    }
                }

                logger.info("search (page {}): {} (size={})", page, fhirQuery, bundle.getTotal());
                if (logger.isDebugEnabled()) {
                    logger.debug("bundle = {}", FhirUtil.toJsonForLogging(bundle));
                }

                compositeBundle.consume(bundle);

                page ++;
            }

            bundle = compositeBundle.getBundle();
        }

        if (validityFunction != null) {
            filterInvalidResources(bundle, validityFunction);
        }

        if (supplementalResourcesFunction != null) {
            appendSupplementalResources(bundle, supplementalResourcesFunction);
        }

        return bundle;
    }

    @SuppressWarnings("unchecked")
    public <T extends IDomainResource> T transact(FHIRCredentialsWithClient fcc, FhirStrategy strategy, T resource) throws Exception {
        IGenericClient client = buildClient(fcc, strategy);

        if (logger.isDebugEnabled()) {
            logger.debug("transacting {}: {}", resource.getClass().getSimpleName(), FhirUtil.toJsonForLogging(resource));
        }

        MethodOutcome outcome = client.create()
                .resource(resource)
                .withAdditionalHeader("Prefer", "return=representation")
                .execute();

        T t;
        try {
            if (outcome.getResource() != null) {
                t = (T) outcome.getResource();

            } else {
                // if a FHIR server doesn't return a resource on the create call itself, it should provide a 'location'
                // header in the response that points to the newly created resource.

                String location = outcome.getResponseHeaders() != null ?
                        outcome.getResponseHeaders().get("location").get(0) :
                        null;

                if (StringUtils.isNotBlank(location)) {
                    String reference = FhirUtil.toRelativeReference(location);
                    t = (T) readByReference(fcc, strategy, resource.getClass(), reference);
                    if (t == null) {
                        throw new ResourceNotFoundException("couldn't find " + reference);
                    }

                } else {
                    throw new ResourceNotFoundException("create did not return a resource, and 'location' not found in response headers");
                }
            }

        } catch (Exception e) {
            logger.error("caught {} transacting {} - {}", e.getClass().getName(), resource.getClass().getSimpleName(), e.getMessage(), e);

            if (logger.isDebugEnabled()) {
                logger.debug("resource={}", FhirUtil.toJsonForLogging(resource));
                logger.debug("outcome={}", outcome);
                if (outcome != null) logger.debug("response status code={}", outcome.getResponseStatusCode());
                if (outcome != null && outcome.getResponseHeaders() != null) {
                    logger.debug("outcome response headers:");
                    for (Map.Entry<String, List<String>> entry : outcome.getResponseHeaders().entrySet() ) {
                        logger.debug("{} : {}", entry.getKey(), StringUtils.join(entry.getValue(), ","));
                    }
                }
                if (logger.isDebugEnabled() && outcome != null && outcome.getOperationOutcome() != null) {
                    logger.debug("response operation outcome={}", FhirUtil.toJsonForLogging(outcome.getOperationOutcome()));
                }
            }

            throw e;
        }

        return t;
    }

    public Bundle transact(FHIRCredentialsWithClient fcc, FhirStrategy strategy, Bundle bundle, boolean stripIfNotInScope) throws Exception {
        IGenericClient client;

        // note : normally, I would reuse the buildClient() function below to build the client, but this version needs to intercept
        //        the AccessToken between creating the JWT and creating the client if we're using the BACKEND strategy, in order to use
        //        the AccessToken to strip unscoped Resources from the Bundle before attempting to write them (which will cause the
        //        operation to blow out with an error).  unfortunately, we can't create the client and then grab the AccessToken from it,
        //        otherwise we could reuse that code.  womp.  whatever.  c'est la vie.

        if (strategy == FhirStrategy.BACKEND) {
            if (accessTokenService.isAccessTokenEnabled()) {
                AccessToken accessToken = accessTokenService.getAccessToken(fcc);

                Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
                while (iter.hasNext()) {
                    Bundle.BundleEntryComponent item = iter.next();
                    Resource resource = item.getResource();
                    if ( ! accessToken.providesWriteAccess(resource.getClass()) ) {
                        if (stripIfNotInScope) {
                            logger.warn("stripping {}/{} from transaction - write permission not in scope", resource.getClass().getSimpleName(), resource.getId());
                            iter.remove();
                        } else {
                            throw new ScopeException("scope does not permit writing " + resource.getClass().getName());
                        }
                    }
                }

                client = FhirUtil.buildClient(fcc.getClient().getFhirContext(), getBackendServerURL(fcc), accessToken.getAccessToken());

            } else {
                throw new ConfigurationException("BACKEND context requested but JWT not defined");
            }

        } else if (strategy == FhirStrategy.PATIENT) {
            client = fcc.getClient();

        } else if (strategy == FhirStrategy.DISABLED) {
            throw new DisabledException("specified strategy is DISABLED");

        } else {
            throw new CaseNotHandledException("case for strategy " + strategy + " not handled");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("transacting Bundle: {}", FhirUtil.toJsonForLogging(bundle));
        }

        Bundle response = client.transaction().withBundle(bundle)
                .withAdditionalHeader("Prefer", "return=representation")
                .execute();

        if (logger.isDebugEnabled()) {
            logger.debug("transaction response: {}", FhirUtil.toJsonForLogging(response));
        }

        return response;
    }



    //////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private boolean canRetry(int attempt) {
        return attempt < maxRetries && ! Thread.currentThread().isInterrupted();
    }

    private String getBackendServerURL(FHIRCredentialsWithClient fcc) {
        return StringUtils.isNotBlank(backendIss) ?
                backendIss :
                fcc.getCredentials().getServerURL();
    }

    private IGenericClient buildClient(FHIRCredentialsWithClient fcc, FhirStrategy strategy) throws DataException, ConfigurationException, IOException {
        if (strategy == FhirStrategy.BACKEND) {
            if (accessTokenService.isAccessTokenEnabled()) {
                AccessToken accessToken = accessTokenService.getAccessToken(fcc);

                return FhirUtil.buildClient(fcc.getClient().getFhirContext(), getBackendServerURL(fcc), accessToken.getAccessToken());

            } else {
                throw new ConfigurationException("BACKEND context requested but JWT not defined");
            }

        } else if (strategy == FhirStrategy.PATIENT) {
            return fcc.getClient();

        } else if (strategy == FhirStrategy.DISABLED) {
            throw new DisabledException("specified strategy is DISABLED");

        } else {
            throw new CaseNotHandledException("case for strategy " + strategy + " not handled");
        }
    }

    private void filterInvalidResources(Bundle bundle, Function<ResourceWithBundle, Boolean> validityFunction) {
        if (bundle != null && bundle.hasEntry()) {
            Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
            while (iter.hasNext()) {
                Bundle.BundleEntryComponent entry = iter.next();
                if (entry.hasResource()) {
                    boolean isValid = validityFunction.apply(new ResourceWithBundle(entry.getResource(), bundle));
                    if ( ! isValid ) {
                        iter.remove();
                    }
                }
            }
        }
    }

    private void appendSupplementalResources(Bundle bundle, Function<ResourceWithBundle, List<Resource>> supplementalResourcesFunction) {
        if (bundle != null && bundle.hasEntry()) {
            List<Resource> resources = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource()) {
                    resources.add(entry.getResource());
                }
            }

            for (Resource resource : resources) {
                try {
                    List<Resource> list = supplementalResourcesFunction.apply(new ResourceWithBundle(resource, bundle));
                    if (list != null) {
                        for (Resource supplementalResource : list) {
                            FhirUtil.appendResourceToBundle(bundle, supplementalResource);
                        }
                    }
                } catch (Exception e) {
                    logger.error("caught {} while appending supplemental resources for {}/{}", e.getClass().getName(), resource.getClass().getSimpleName(), resource.getId(), e);
                    if (e instanceof AuthenticationException ae) throw ae;
                }
            }
        }
    }
}
