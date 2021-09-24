package edu.ohsu.cmp.coach.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
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
        int index = reference.indexOf('/');
        String referenceId = index >= 0 ?
                reference.substring(index + 1) :
                reference;

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            Resource r = entry.getResource();
            if (r.getClass().isAssignableFrom(aClass)) {
                if (r.hasId()) {
                    try {
                        if (Pattern.matches("(.*\\/)?" + referenceId + "(\\/.*)?", r.getId())) {
                            return aClass.cast(entry.getResource());
                        }
                    } catch (NullPointerException npe) {
                        logger.error("caught " + npe.getClass().getName() + " matching reference '" + reference +
                                "' against id '" + r.getId() + "'", npe);
                        throw npe;
                    }
                }
            }
        }
        return null;
    }

    public static boolean bundleContainsReference(Bundle b, String reference) {
        int index = reference.indexOf('/');
        String referenceId = index >= 0 ?
                reference.substring(index + 1) :
                reference;

        for (Bundle.BundleEntryComponent entry : b.getEntry()) {
            Resource r = entry.getResource();
            if (r.hasId()) {
                try {
                    if (Pattern.matches("(.*\\/)?" + referenceId + "(\\/.*)?", r.getId())) {
                        logger.debug("matched: '" + r.getId() + "' contains '" + reference + "'");
                        return true;
                    } else {
                        logger.debug("did not match: '" + r.getId() + "' does not contain '" + referenceId + "'");
                    }
                } catch (NullPointerException npe) {
                    logger.error("caught " + npe.getClass().getName() + " matching reference '" + referenceId +
                            "' against id '" + r.getId() + "'", npe);
                    throw npe;
                }
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
