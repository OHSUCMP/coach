package edu.ohsu.cmp.htnu18app.registry;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.ohsu.cmp.htnu18app.model.FHIRCredentials;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FHIRRegistry {

    private static FHIRRegistry registry = null;

    public static FHIRRegistry getInstance() {
        if (registry == null) {
            registry = new FHIRRegistry();
        }
        return registry;
    }

////////////////////////////////////////////////////////////////////////////

    private final Map<String, FHIRCredentialsWithClient> map = new ConcurrentHashMap<String, FHIRCredentialsWithClient>();
    private final FhirContext ctx = FhirContext.forR4();

    private FHIRRegistry() {
        // private constructor, singleton class
    }

    synchronized public boolean set(String sessionId, FHIRCredentials credentials) {
        if ( ! map.containsKey(sessionId) ) {
            IGenericClient client = buildClient(credentials);
            map.put(sessionId, new FHIRCredentialsWithClient(credentials, client));
            return true;
        }
        return false;
    }

    public FHIRCredentials getCredentials(String sessionId) {
        return map.get(sessionId).getCredentials();
    }

    public IGenericClient getClient(String sessionId) {
        return map.get(sessionId).getClient();
    }

    public boolean exists(String sessionId) {
        return map.containsKey(sessionId);
    }

    public boolean remove(String sessionId) {
        if (map.containsKey(sessionId)) {
            map.remove(sessionId);
            return true;
        }
        return false;
    }

    private IGenericClient buildClient(FHIRCredentials credentials) {
        IGenericClient client = ctx.newRestfulGenericClient(credentials.getServerURL());

        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(credentials.getBearerToken());
        client.registerInterceptor(authInterceptor);

        return client;
    }

////////////////////////////////////////////////////////////////////////

    private static final class FHIRCredentialsWithClient {
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
    }
}
