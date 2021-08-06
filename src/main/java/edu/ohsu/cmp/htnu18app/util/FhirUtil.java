package edu.ohsu.cmp.htnu18app.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IQuery;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import org.hl7.fhir.instance.model.api.IBaseBundle;
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

//    public static Resource getResourceByUrl(String sessionId, String url) {
//        CacheData cache = SessionCache.getInstance().get(sessionId);
//        Resource r = cache.getResource(url);
//        if (r == null) {
//            logger.info("getting Resource with url=" + url + " for session " + sessionId);
//            Bundle b = cache.getFhirCredentialsWithClient().getClient().search()
//                    .byUrl(url)
//                    .returnBundle(Bundle.class)
//                    .execute();
//            r = b.getEntryFirstRep().getResource();
//            cache.setResource(url, r);
//        }
//        return r;
//    }

    public static Resource getResourceById(String sessionId, String id) {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        Resource r = cache.getResource(id);
        if (r == null) {
            logger.info("getting Resource with id=" + id + " for session " + sessionId);
            String url = cache.getFhirCredentialsWithClient().getCredentials().getServerURL() + '/' + id;
            Bundle b = cache.getFhirCredentialsWithClient().getClient().search()
                    .byUrl(url)
                    .returnBundle(Bundle.class)
                    .execute();
            r = b.getEntryFirstRep().getResource();
            cache.setResource(id, r);
        }
        return r;
    }

    // search function to facilitate getting large datasets involving multi-paginated queries
    public static Bundle search(FHIRCredentialsWithClient fcc, IQuery<IBaseBundle> query) {
        Bundle bundle = query.returnBundle(Bundle.class).execute();

        if (bundle.getLink(Bundle.LINK_NEXT) == null) {
            return bundle;

        } else {
            Bundle compositeBundle = new Bundle();
            compositeBundle.setType(Bundle.BundleType.COLLECTION);

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                compositeBundle.getEntry().add(entry);
            }

            while (bundle.getLink(Bundle.LINK_NEXT) != null) {
                bundle = fcc.getClient().loadPage().next(bundle).execute();
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    compositeBundle.getEntry().add(entry);
                }
            }


            return compositeBundle;
        }
    }


}
