package edu.ohsu.cmp.htnu18app.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.regex.Pattern;

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

    public static <T extends IBaseResource> T getResourceFromBundleByReference(Bundle b, Class<T> aClass, String reference) {
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            String id = entry.getResource().getId();
            if (id == null) continue;
            try {
                if (Pattern.matches(".*\\/" + reference + "\\/.*", id)) {
                    return aClass.cast(entry.getResource());
                }
            } catch (NullPointerException npe) {
                logger.error("caught " + npe.getClass().getName() + " matching reference '" + reference +
                        "' against id '" + id + "'", npe);
                throw npe;
            }
        }
        return null;
    }

    public static boolean bundleContainsReference(Bundle b, String reference) {
        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            String id = entry.getResource().getId();
            if (id == null) continue;
            try {
                if (Pattern.matches(".*\\/" + reference + "\\/.*", id)) {
                    logger.debug("matched: '" + reference + "' == '" + id + "'");
                    return true;
                } else {
                    logger.debug("did not match: '" + reference + "' != '" + id + "'");
                }
            } catch (NullPointerException npe) {
                logger.error("caught " + npe.getClass().getName() + " matching reference '" + reference +
                        "' against id '" + id + "'", npe);
                throw npe;
            }
        }
        return false;
    }

    public static String toJson(IBaseResource r) {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        parser.setPrettyPrint(true);
        return parser.encodeResourceToString(r);
    }
}
