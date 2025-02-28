package edu.ohsu.cmp.coach.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.ohsu.cmp.coach.exception.*;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.fhir.FhirStrategy;
import edu.ohsu.cmp.coach.model.ResourceWithBundle;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.coach.model.fhir.jwt.AccessToken;
import edu.ohsu.cmp.coach.util.FhirUtil;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class FHIRService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${socket.timeout:300000}")
    private Integer socketTimeout;

    @Value("${fhir.search.count}")
    private int searchCount;

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
        String identifierString = FhirUtil.toIdentifierString(identifier);
        Bundle b = search(fcc, strategy, aClass.getSimpleName() + "/?identifier=" + identifierString);

        if (b.getEntry().isEmpty()) {
            logger.warn("couldn't find resource with identifier=" + identifierString);
            return null;
        }

        Resource r = null;

        try {
            r = b.getEntryFirstRep().getResource();

            if (b.getEntry().size() == 1) {
                logger.debug("found " + r.getClass().getName() + " with identifier=" + identifierString);

            } else {
                logger.warn("found " + b.getEntry().size() + " resources associated with identifier=" + identifierString +
                        "!  returning first match (" + r.getClass().getName() + ") -");
            }

            return aClass.cast(r);

        } catch (ClassCastException cce) {
            logger.error("caught " + cce.getClass().getName() + " attempting to cast " + r.getClass().getName() + " to " + aClass.getName());
            logger.debug(r.getClass().getName() + " : " + FhirUtil.toJson(r));
            throw cce;
        }
    }

    public <T extends IBaseResource> T readByReference(FHIRCredentialsWithClient fcc, FhirStrategy strategy, Class<T> aClass,
                                                       String reference) throws DataException, ConfigurationException, IOException {
        logger.info("read: " + reference + " (" + aClass.getSimpleName() + ")");
        String id = FhirUtil.extractIdFromReference(reference);
        IGenericClient client = buildClient(fcc, strategy);
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

    // search function to facilitate getting large datasets involving multi-paginated queries
    public Bundle search(FHIRCredentialsWithClient fcc, FhirStrategy strategy, String fhirQuery) throws DataException, ConfigurationException, IOException {
        return search(fcc, strategy, fhirQuery, null);
    }

    public Bundle search(FHIRCredentialsWithClient fcc, FhirStrategy strategy, String fhirQuery,
                         Function<ResourceWithBundle, Boolean> validityFunction) throws DataException, ConfigurationException, IOException {

        if (StringUtils.isBlank(fhirQuery) || strategy == FhirStrategy.DISABLED) {
            return null;
        }

        logger.info("search: executing query: " + fhirQuery);

        IGenericClient client = buildClient(fcc, strategy);

        Bundle bundle;
        try {
            bundle = client.search()
                    .byUrl(fcc.getCredentials().getServerURL() + '/' + fhirQuery)
                    .count(searchCount)
                    .accept("application/fhir+json")        // required for Cerner
                    .returnBundle(Bundle.class)
                    .execute();

            // bundle.getTotal() may be null and if so it will return 0, even if there are many entries.  Cerner does this
            logger.info("search: got Bundle with total=" + bundle.getTotal() + ", entries=" + bundle.getEntry().size() + " for query: " + fhirQuery);
            if (logger.isDebugEnabled()) {
                logger.debug("bundle = " + FhirUtil.toJson(bundle));
            }

        } catch (InvalidRequestException ire) {
            logger.error("caught " + ire.getClass().getName() + " executing search: " + fhirQuery, ire);
            throw ire;
        }

        if (bundle.getLink(Bundle.LINK_NEXT) != null) {
            CompositeBundle compositeBundle = new CompositeBundle();
            compositeBundle.consume(bundle);

            int page = 2;
            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = fcc.getClient().loadPage().next(bundle).execute();

                logger.info("search (page " + page + "): " + fhirQuery + " (size=" + bundle.getTotal() + ")");
                if (logger.isDebugEnabled()) {
                    logger.debug("bundle = " + FhirUtil.toJson(bundle));
                }

                compositeBundle.consume(bundle);

                page ++;
            }

            bundle = compositeBundle.getBundle();
        }

        if (validityFunction != null) {
            filterInvalidResources(bundle, validityFunction);
        }
        return bundle;
    }

    public <T extends IDomainResource> T transact(FHIRCredentialsWithClient fcc, FhirStrategy strategy, T resource) throws IOException, ConfigurationException, DataException {
        IGenericClient client = buildClient(fcc, strategy);

        if (logger.isDebugEnabled()) {
            logger.debug("transacting " + resource.getClass().getSimpleName() + ": " + FhirUtil.toJson(resource));
        }

        MethodOutcome outcome = client.create()
                .resource(resource)
                .withAdditionalHeader("Prefer", "return=representation")
                .execute();

        T t = null;
        try {
            t = (T) outcome.getResource();

            if (logger.isDebugEnabled()) {
                logger.debug("received response " + t.getClass().getSimpleName() + ": " + FhirUtil.toJson(t));
            }

        } catch (NullPointerException npe) {
            logger.error("Outcome contained a null resource in response to " + resource.getClass().getSimpleName() + " creation!");
            if (logger.isDebugEnabled()) {
                logger.debug("outcome=" + outcome);
                if (outcome != null) logger.debug("response status code=" + outcome.getResponseStatusCode());
                if (outcome != null && outcome.getResponseHeaders() != null) {
                    logger.debug("outcome response headers:");
                    for (Map.Entry<String, List<String>> entry : outcome.getResponseHeaders().entrySet() ) {
                        logger.debug(entry.getKey() + "=" + StringUtils.join(entry.getValue(), ","));
                    }
                }
                if (outcome != null && outcome.getOperationOutcome() != null) {
                    logger.debug("response operation outcome=" + FhirUtil.toJson(outcome.getOperationOutcome()));
                }
            }

            throw npe;
        }

        return t;
    }

    public Bundle transact(FHIRCredentialsWithClient fcc, FhirStrategy strategy, Bundle bundle, boolean stripIfNotInScope) throws IOException, DataException, ConfigurationException, ScopeException {
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
                            logger.warn("stripping " + resource.getClass().getName() + " with id=" + resource.getId() + " from transaction - write permission not in scope");
                            iter.remove();
                        } else {
                            throw new ScopeException("scope does not permit writing " + resource.getClass().getName());
                        }
                    }
                }

                client = FhirUtil.buildClient(getBackendServerURL(fcc),
                        accessToken.getAccessToken(),
                        socketTimeout);

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



//////////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private String getBackendServerURL(FHIRCredentialsWithClient fcc) {
        return StringUtils.isNotBlank(backendIss) ?
                backendIss :
                fcc.getCredentials().getServerURL();
    }

    private IGenericClient buildClient(FHIRCredentialsWithClient fcc, FhirStrategy strategy) throws DataException, ConfigurationException, IOException {
        if (strategy == FhirStrategy.BACKEND) {
            if (accessTokenService.isAccessTokenEnabled()) {
                AccessToken accessToken = accessTokenService.getAccessToken(fcc);

                return FhirUtil.buildClient(getBackendServerURL(fcc),
                        accessToken.getAccessToken(),
                        socketTimeout);

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
}
