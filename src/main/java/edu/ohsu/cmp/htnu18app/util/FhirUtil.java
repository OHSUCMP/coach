package edu.ohsu.cmp.htnu18app.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class FhirUtil {
    private static final Logger logger = LoggerFactory.getLogger(FhirUtil.class);

    public static IGenericClient buildClient(String serverUrl, String bearerToken) {
        FhirContext ctx = FhirContext.forR4();
        IGenericClient client = ctx.newRestfulGenericClient(serverUrl);

        BearerTokenAuthInterceptor authInterceptor = new BearerTokenAuthInterceptor(bearerToken);
        client.registerInterceptor(authInterceptor);

        return client;
    }

    public static void writeBundleTOC(Logger logger, Bundle bundle) {
        Iterator<Bundle.BundleEntryComponent> iter = bundle.getEntry().iterator();
        int i = 0;
        while (iter.hasNext()) {
            Bundle.BundleEntryComponent entry = iter.next();
            logger.info(i + ": " + entry.getResource().getClass().getName() + " (" + entry.getResource().getId() + ")");
            i ++;
        }
    }

    public static Resource getResourceByUrl(String sessionId, String url) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Resource r = cache.getResource(url);
        if (r == null) {
            logger.info("getting Resource with url=" + url + " for session " + sessionId);
            Bundle b = cache.getFhirCredentialsWithClient().getClient().search()
                    .byUrl(url)
                    .returnBundle(Bundle.class)
                    .execute();
            r = b.getEntryFirstRep().getResource();
            cache.setResource(url, r);
        }
        return r;
    }
}
