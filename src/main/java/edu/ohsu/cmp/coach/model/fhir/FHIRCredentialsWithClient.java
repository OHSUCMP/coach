package edu.ohsu.cmp.coach.model.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.exception.DataException;
import edu.ohsu.cmp.coach.exception.ScopeException;
import edu.ohsu.cmp.coach.fhir.CompositeBundle;
import edu.ohsu.cmp.coach.model.fhir.jwt.AccessToken;
import edu.ohsu.cmp.coach.service.JWTService;
import edu.ohsu.cmp.coach.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

public class FHIRCredentialsWithClient implements IFHIRCredentialsWithClient {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private FHIRCredentials credentials;
    private IGenericClient client;
    private CapabilityStatement metadata = null;

    public FHIRCredentialsWithClient(FHIRCredentials credentials, IGenericClient client) {
        this.credentials = credentials;
        this.client = client;
    }

    @Override
    public IFHIRCredentials getCredentials() {
        return credentials;
    }

    private IGenericClient getClient() {
        return client;
    }

    @Override
    public <T extends IBaseResource> T read(Class<T> aClass, String id) {
        return getClient().read()
                .resource(aClass)
                .withId(id)
                .execute();
    }

    @Override
    public Bundle search(String query, int count, String accept) {
        Bundle bundle = getClient().search()
                .byUrl(credentials.getServerURL() + '/' + query)
                .count(count)
                .accept(accept) // required for Cerner
                .returnBundle(Bundle.class)
                .execute();

        if (bundle.getLink(Bundle.LINK_NEXT) == null) {
            return bundle;

        } else {
            CompositeBundle compositeBundle = new CompositeBundle();
            compositeBundle.consume(bundle);

            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = getClient().loadPage().next(bundle).execute();
                compositeBundle.consume(bundle);
            }

            return compositeBundle.getBundle();
        }
    }

    @Override
    public <T extends IDomainResource> T transact(JWTService jwtService, T resource, Integer socketTimeout) throws IOException, ConfigurationException, DataException {
        IGenericClient client;
        if (jwtService.isJWTEnabled()) {
            String tokenAuthUrl = FhirUtil.getTokenAuthenticationURL(getMetadata());
            String jwt = jwtService.createToken(tokenAuthUrl);
            AccessToken accessToken = jwtService.getAccessToken(tokenAuthUrl, jwt);

            client = FhirUtil.buildClient(getCredentials().getServerURL(),
                    accessToken.getAccessToken(),
                    socketTimeout);
        } else {
            client = getClient();
        }

        MethodOutcome outcome = client.create()
                .resource(resource)
                .withAdditionalHeader("Prefer", "return=representation")
                .execute();

        T t = (T) outcome.getResource();

        return t;
    }

    @Override
    public Bundle transact(JWTService jwtService, Bundle bundle, boolean stripIfNotInScope, Integer socketTimeout) throws IOException, DataException, ConfigurationException, ScopeException {
        IGenericClient client;
        if (jwtService.isJWTEnabled()) {
            String tokenAuthUrl = FhirUtil.getTokenAuthenticationURL(getMetadata());
            String jwt = jwtService.createToken(tokenAuthUrl);
            AccessToken accessToken = jwtService.getAccessToken(tokenAuthUrl, jwt);

            filterByScope(bundle, accessToken, stripIfNotInScope);

            client = FhirUtil.buildClient(getCredentials().getServerURL(),
                    accessToken.getAccessToken(),
                    socketTimeout);
        } else {
            client = getClient();
        }

        return client.transaction().withBundle(bundle)
                .withAdditionalHeader("Prefer", "return=representation")
                .execute();
    }


//////////////////////////////////////////////////////////////////////////////////
// private methods
//

    private CapabilityStatement getMetadata() {
        if (metadata == null && client != null) {
            metadata = client.capabilities()
                    .ofType(CapabilityStatement.class)
                    .execute();
        }
        return metadata;
    }

    private void filterByScope(Bundle bundle, AccessToken accessToken, boolean stripIfNotInScope) throws ScopeException {
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
    }
}
